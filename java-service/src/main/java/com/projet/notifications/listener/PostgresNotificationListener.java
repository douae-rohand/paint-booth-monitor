package com.projet.notifications.listener;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.gateway.dto.AlerteMessage;
import com.projet.gateway.dto.KpiMessage;
import com.projet.kpis.service.KpiService;
import com.projet.notifications.service.NotificationDispatchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener PostgreSQL NOTIFY pour les alertes.
 *
 * Écoute deux canaux sur la même connexion JDBC dédiée (hors pool HikariCP) :
 *   - nouvelle_alerte : dispatch création → WebSocket + outbox email
 *   - alerte_resolue  : publication WebSocket "RESOLUTION" → AppShell et
 *                       ActiveAlertsBand rafraîchissent fetchAlertesActives()
 *
 * La connexion dédiée est nécessaire car HikariCP recycle les connexions,
 * ce qui est incompatible avec LISTEN qui doit rester ouvert en continu.
 * PGConnection.getNotifications() retourne toutes les notifications en attente
 * sur tous les canaux EXX sur cette connexion — un seul thread suffit.
 */
@Component
public class PostgresNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(PostgresNotificationListener.class);
    private static final String CHANNEL_CREATION  = "nouvelle_alerte";
    private static final String CHANNEL_RESOLUTION = "alerte_resolue";
    private static final long NOTIFICATION_TIMEOUT_MS = 5000;
    private static final long MAX_RECONNECT_DELAY_MS = 60000;

    private final DataSource dataSource;
    private final NotificationDispatchService notificationDispatchService;
    private final AlerteRepository alerteRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KpiService kpiService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private Connection dedicatedConnection;
    private PGConnection pgConnection;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int consecutiveErrors = 0;

    public PostgresNotificationListener(
            DataSource dataSource,
            NotificationDispatchService notificationDispatchService,
            AlerteRepository alerteRepository,
            SimpMessagingTemplate messagingTemplate,
            KpiService kpiService
    ) {
        this.dataSource = dataSource;
        this.notificationDispatchService = notificationDispatchService;
        this.alerteRepository = alerteRepository;
        this.messagingTemplate = messagingTemplate;
        this.kpiService = kpiService;
    }

    @PostConstruct
    public void start() {
        logger.info("Démarrage du listener PostgreSQL NOTIFY sur les canaux '{}' et '{}'",
                CHANNEL_CREATION, CHANNEL_RESOLUTION);
        running.set(true);
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "postgres-notification-listener");
            thread.setDaemon(false);
            return thread;
        });
        executorService.submit(this::listenLoop);
    }

    @PreDestroy
    public void stop() {
        logger.info("Arrêt du listener PostgreSQL NOTIFY");
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        closeConnection();
    }

    private void listenLoop() {
        while (running.get()) {
            try {
                ensureConnection();

                PGNotification[] notifications = pgConnection.getNotifications((int) NOTIFICATION_TIMEOUT_MS);

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        handleNotification(notification);
                    }
                }

                if (consecutiveErrors > 0) {
                    logger.info("Connexion rétablie après {} échecs consécutifs", consecutiveErrors);
                    consecutiveErrors = 0;
                }

            } catch (SQLException e) {
                consecutiveErrors++;
                logger.error("Erreur lors de l'écoute PostgreSQL (tentative {})", consecutiveErrors, e);

                if (consecutiveErrors >= 5) {
                    logger.error("{} échecs consécutifs - prochaine tentative dans {}s",
                            consecutiveErrors, calculateReconnectDelay() / 1000);
                }

                closeConnection();

                long delay = calculateReconnectDelay();
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void ensureConnection() throws SQLException {
        if (dedicatedConnection == null || dedicatedConnection.isClosed()) {
            logger.info("Tentative de connexion PostgreSQL dédiée pour LISTEN");
            dedicatedConnection = java.sql.DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            pgConnection = dedicatedConnection.unwrap(PGConnection.class);

            // Les deux LISTEN sur la même connexion — getNotifications() retourne
            // toutes les notifications en attente sur les deux canaux à chaque appel.
            try (Statement stmt = dedicatedConnection.createStatement()) {
                stmt.execute("LISTEN " + CHANNEL_CREATION);
                stmt.execute("LISTEN " + CHANNEL_RESOLUTION);
            }

            logger.info("Connexion dédiée établie — LISTEN '{}' et '{}' activés",
                    CHANNEL_CREATION, CHANNEL_RESOLUTION);
        }
    }

    private void handleNotification(PGNotification notification) {
        String channel = notification.getName();
        String payload  = notification.getParameter();

        logger.debug("Notification reçue sur canal '{}' avec payload: '{}'", channel, payload);

        if (payload == null) {
            logger.warn("Payload null reçu sur canal '{}' — ignoré", channel);
            return;
        }

        UUID idAlerte;
        try {
            idAlerte = UUID.fromString(payload.trim());
        } catch (IllegalArgumentException e) {
            logger.error("Payload invalide sur canal '{}': '{}'", channel, payload, e);
            return;
        }

        if (CHANNEL_CREATION.equals(channel)) {
            logger.info("Alerte {} reçue via NOTIFY (création), dispatch en cours", idAlerte);
            notificationDispatchService.dispatcherAlerte(idAlerte);

        } else if (CHANNEL_RESOLUTION.equals(channel)) {
            logger.info("Alerte {} reçue via NOTIFY (résolution), publication WebSocket", idAlerte);
            publishResolutionWebSocket(idAlerte);
        }
    }

    /**
     * Publie un message "RESOLUTION" sur /topic/alertes et recalcule/publie les KPI
     * sur /topic/kpis — symétrique au comportement de dispatcherAlerte pour la création.
     *
     * La même méthode getKpisGlobaux() est réutilisée sans duplication : 3 COUNT indexés,
     * négligeable au volume d'une cabine de peinture.
     * KpiSection réagit de façon générique à tout message sur /topic/kpis — aucune
     * modification frontend nécessaire.
     */
    private void publishResolutionWebSocket(UUID idAlerte) {
        alerteRepository.findById(idAlerte).ifPresentOrElse(alerte -> {
            // 1. Publier sur /topic/alertes — AppShell et ActiveAlertsBand appellent fetchAlertesActives()
            AlerteMessage alerteMessage = new AlerteMessage(
                    "RESOLUTION",
                    alerte.getIdAlerte(),
                    null,
                    null,
                    alerte.getMetrique(),
                    alerte.getTypeAlerte(),
                    alerte.getSeverite(),
                    alerte.getUpdatedAt()
            );
            messagingTemplate.convertAndSend("/topic/alertes", alerteMessage);
            logger.info("[WS] Alerte {} (RESOLUTION) publiée sur /topic/alertes", idAlerte);

            // 2. Recalculer et publier les KPI — symétrique à publishKpisWebSocket() pour la création.
            // alertesActives diminue après résolution : KpiSection doit se mettre à jour en temps réel.
            var kpis = kpiService.getKpisGlobaux();
            KpiMessage kpiMessage = new KpiMessage(kpis.getAlertesActives(), kpis.getNbPointsEnAnomalie());
            messagingTemplate.convertAndSend("/topic/kpis", kpiMessage);
            logger.info("[WS] KPI recalculés après résolution alerte {} — alertesActives={}",
                    idAlerte, kpis.getAlertesActives());

        }, () -> logger.warn("Alerte {} introuvable lors de la résolution — WS ignoré", idAlerte));
    }

    private void closeConnection() {
        if (dedicatedConnection != null) {
            try {
                if (!dedicatedConnection.isClosed()) {
                    dedicatedConnection.close();
                }
                logger.info("Connexion dédiée fermée");
            } catch (SQLException e) {
                logger.warn("Erreur lors de la fermeture de la connexion dédiée", e);
            } finally {
                dedicatedConnection = null;
                pgConnection = null;
            }
        }
    }

    private long calculateReconnectDelay() {
        if (consecutiveErrors == 1) return 0;
        long delay = 5000 * (long) Math.pow(2, consecutiveErrors - 2);
        return Math.min(delay, MAX_RECONNECT_DELAY_MS);
    }
}
