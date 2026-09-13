package com.projet.alerting.service;

import com.projet.alerting.model.Alerte;
import com.projet.gateway.dto.AlerteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service dédié au broadcast WebSocket global des alertes.
 *
 * Responsabilité unique : publier sur /topic/alertes lors de chaque
 * création ou résolution d'alerte, pour alimenter le dashboard temps réel
 * (ActiveAlertsBand, AlertesPage, KpiSection via subscribeToAlertes).
 *
 * KpiSection rafraîchit ses KPIs en re-fetchant GET /api/kpis après
 * chaque signal /topic/alertes — le broadcast séparé /topic/kpis a été
 * supprimé car aucun composant frontend ne l'abonnait.
 *
 * Pas d'interface — un seul mécanisme de broadcast (WebSocket STOMP).
 */
@Service
public class AlerteBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(AlerteBroadcastService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AlerteBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publie l'alerte sur /topic/alertes.
     *
     * @param alerte         l'alerte concernée (déjà chargée par l'appelant)
     * @param evenement      "CREATION" ou "RESOLUTION"
     * @param idPointMesure  ID du point de mesure (null acceptable pour RESOLUTION)
     * @param nomPointMesure nom du point de mesure (null acceptable pour RESOLUTION)
     */
    public void publierAlerte(
            Alerte alerte,
            String evenement,
            Long idPointMesure,
            String nomPointMesure
    ) {
        AlerteMessage alerteMessage = new AlerteMessage(
                evenement,
                alerte.getIdAlerte(),
                idPointMesure,
                nomPointMesure,
                alerte.getMetrique(),
                alerte.getTypeAlerte(),
                alerte.getSeverite(),
                "CREATION".equals(evenement) ? alerte.getCreatedAt() : alerte.getUpdatedAt()
        );
        messagingTemplate.convertAndSend("/topic/alertes", alerteMessage);
        logger.info("[WS] Alerte {} ({}) publiée sur /topic/alertes", alerte.getIdAlerte(), evenement);
    }
}
