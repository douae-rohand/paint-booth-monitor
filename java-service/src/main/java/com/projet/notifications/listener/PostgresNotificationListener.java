package com.projet.notifications.listener;

import com.projet.notifications.service.NotificationDispatchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Ce composant écoute les notifications PostgreSQL sur le canal "nouvelle_alerte"
 * via une connexion dédiée (hors pool HikariCP) et dispatche les alertes reçues.
 * 
 * La connexion dédiée est nécessaire car le pool HikariCP recycle les connexions,
 * ce qui est incompatible avec un LISTEN qui doit rester ouvert en continu.
 */
@Component
public class PostgresNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(PostgresNotificationListener.class);
    private static final String CHANNEL = "nouvelle_alerte";
    private static final long NOTIFICATION_TIMEOUT_MS = 5000;
    private static final long MAX_RECONNECT_DELAY_MS = 60000;

    private final DataSource dataSource;
    private final NotificationDispatchService notificationDispatchService;

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
            NotificationDispatchService notificationDispatchService
    ) {
        this.dataSource = dataSource;
        this.notificationDispatchService = notificationDispatchService;
    }

    @PostConstruct
    public void start() {
        logger.info("Démarrage du listener PostgreSQL NOTIFY sur le canal '{}'", CHANNEL);
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
                
                // Écouter les notifications avec timeout
                PGNotification[] notifications = pgConnection.getNotifications((int) NOTIFICATION_TIMEOUT_MS);
                
                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        handleNotification(notification);
                    }
                }
                
                // Réinitialiser le compteur d'erreurs après succès
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
                
                // Backoff exponentiel
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
            
            try (Statement stmt = dedicatedConnection.createStatement()) {
                stmt.execute("LISTEN " + CHANNEL);
            }
            
            logger.info("Connexion dédiée établie et LISTEN '{}' activé", CHANNEL);
        }
    }

    private void handleNotification(PGNotification notification) {
        String channel = notification.getName();
        String payload = notification.getParameter();
        
        logger.debug("Notification reçue sur canal '{}' avec payload: '{}'", channel, payload);
        
        if (CHANNEL.equals(channel) && payload != null) {
            try {
                UUID idAlerte = UUID.fromString(payload.trim());
                logger.info("Alerte {} reçue via NOTIFY, dispatch en cours", idAlerte);
                notificationDispatchService.dispatcherAlerte(idAlerte);
            } catch (IllegalArgumentException e) {
                logger.error("Payload invalide pour l'alerte: '{}'", payload, e);
            }
        }
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
        // Backoff exponentiel: immédiat, 5s, 10s, 30s, max 60s
        if (consecutiveErrors == 1) {
            return 0; // Tentative immédiate
        }
        long delay = 5000 * (long) Math.pow(2, consecutiveErrors - 2);
        return Math.min(delay, MAX_RECONNECT_DELAY_MS);
    }
}
