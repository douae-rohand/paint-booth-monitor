package com.projet.gateway.listener;

import com.projet.alerting.model.enums.Metrique;
import com.projet.gateway.dto.MesureMessage;
import com.projet.gateway.dto.StatutTempsReelMessage;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import com.projet.measures.service.StatutTempsReelService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener PostgreSQL NOTIFY pour les nouvelles mesures.
 * 
 * Ce composant écoute les notifications PostgreSQL sur le canal "nouvelle_mesure"
 * via une connexion dédiée (hors pool HikariCP) et publie les messages WebSocket.
 * 
 * Payload format: "<id_mesure>"
 */
@Component
public class MesureNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(MesureNotificationListener.class);
    private static final String CHANNEL = "nouvelle_mesure";
    private static final long NOTIFICATION_TIMEOUT_MS = 5000;
    private static final long MAX_RECONNECT_DELAY_MS = 60000;

    private final MesureRepository mesureRepository;
    private final PointMesureRepository pointMesureRepository;
    private final StatutTempsReelService statutTempsReelService;
    private final SimpMessagingTemplate messagingTemplate;

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

    public MesureNotificationListener(
            MesureRepository mesureRepository,
            PointMesureRepository pointMesureRepository,
            StatutTempsReelService statutTempsReelService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.mesureRepository = mesureRepository;
        this.pointMesureRepository = pointMesureRepository;
        this.statutTempsReelService = statutTempsReelService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void start() {
        logger.info("Démarrage du listener PostgreSQL NOTIFY sur le canal '{}'", CHANNEL);
        running.set(true);
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "postgres-mesure-notification-listener");
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
                UUID idMesure = UUID.fromString(payload.trim());
                logger.info("Mesure {} reçue via NOTIFY, publication WebSocket en cours", idMesure);
                publishMesureWebSocket(idMesure);
            } catch (IllegalArgumentException e) {
                logger.error("Payload invalide pour la mesure: '{}'", payload, e);
            }
        }
    }

    public void publishMesureWebSocket(UUID idMesure) {
        logger.info("=== DÉBUT publishMesureWebSocket pour mesure {} ===", idMesure);
        logger.info("Recherche de mesure {} dans la base de données", idMesure);
        // Récupérer la mesure depuis la base avec JOIN FETCH pour éviter LazyInitializationException
        try {
            var mesure = mesureRepository.findByIdWithPointMesure(idMesure);
            if (mesure != null) {
                PointMesure pointMesure = mesure.getPointMesure();
                if (pointMesure == null) {
                    logger.warn("Point de mesure non trouvé pour la mesure {}", idMesure);
                    return;
                }

                logger.info("Mesure trouvée: pointMesureId={}, metrique={}, valeur={}", pointMesure.getId(), mesure.getMetrique(), mesure.getValeur());

                // Publier sur /topic/mesures/{idPointMesure}/{metrique}
                String topic = String.format("/topic/mesures/%d/%s",
                        pointMesure.getId(),
                        mesure.getMetrique().name());

                MesureMessage message = new MesureMessage(
                        pointMesure.getId(),
                        pointMesure.getNom(),
                        mesure.getMetrique(),
                        mesure.getValeur(),
                        mesure.getCreatedAt()
                );

                logger.info("Tentative de publication sur topic {}", topic);
                try {
                    messagingTemplate.convertAndSend(topic, message);
                    logger.info("Message publié avec succès sur {}", topic);
                } catch (Exception e) {
                    logger.error("Erreur lors de la publication sur {}: {}", topic, e.getMessage(), e);
                }

                // Publier le statut temps réel recalculé pour ce point
                logger.info("Appel de publishStatutTempsReel pour point {}", pointMesure.getId());
                try {
                    publishStatutTempsReel(pointMesure.getId());
                    logger.info("publishStatutTempsReel terminé");
                } catch (Exception e) {
                    logger.error("Erreur lors de publishStatutTempsReel: {}", e.getMessage(), e);
                }
                logger.info("=== FIN publishMesureWebSocket ===");
            } else {
                logger.warn("Mesure {} non trouvée dans la base de données", idMesure);
                logger.info("=== FIN publishMesureWebSocket (mesure non trouvée) ===");
            }
        } catch (Exception e) {
            logger.error("Exception dans publishMesureWebSocket: {}", e.getMessage(), e);
        }
    }

    private void publishStatutTempsReel(Long idPointMesure) {
        // Récupérer le statut temps réel pour tous les points
        var statuts = statutTempsReelService.getStatutTousPoints();
        logger.info("Publication statut temps réel pour point {}, nombre de statuts disponibles: {}", idPointMesure, statuts.size());
        
        // Trouver le statut pour ce point spécifique
        statuts.stream()
                .filter(s -> s.getIdPointMesure().equals(idPointMesure))
                .findFirst()
                .ifPresentOrElse(statut -> {
                    StatutTempsReelMessage message = new StatutTempsReelMessage(
                            statut.getIdPointMesure(),
                            statut.getNomPointMesure(),
                            statut.getTypeEmplacement(),
                            statut.getMesures()
                    );
                    messagingTemplate.convertAndSend("/topic/statut-temps-reel", message);
                    logger.info("Statut temps réel publié pour point {} sur /topic/statut-temps-reel", idPointMesure);
                }, () -> {
                    logger.warn("Statut temps réel non trouvé pour point {} dans la liste de {} statuts", idPointMesure, statuts.size());
                });
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
