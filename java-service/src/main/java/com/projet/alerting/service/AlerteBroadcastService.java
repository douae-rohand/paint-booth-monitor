package com.projet.alerting.service;

import com.projet.alerting.model.Alerte;
import com.projet.gateway.dto.AlerteMessage;
import com.projet.gateway.dto.KpiMessage;
import com.projet.kpis.service.KpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service dédié au broadcast WebSocket global des alertes et KPI.
 *
 * Responsabilité unique : publier sur les topics globaux (/topic/alertes, /topic/kpis)
 * qui alimentent le dashboard temps réel de tous les clients connectés simultanément
 * (ActiveAlertsBand, AlertesPage, KpiSection, HeatmapSection).
 *
 * Pas d'interface — un seul mécanisme de broadcast (WebSocket STOMP), pas de fournisseur
 * alternatif prévu contrairement à EmailService.
 *
 * Appelé de façon symétrique pour CREATION et RESOLUTION depuis PostgresNotificationListener :
 *   CHANNEL_CREATION  → notificationDispatchService.dispatcherAlerte()
 *                       alerteBroadcastService.publierAlerteEtKpis(alerte, "CREATION")
 *   CHANNEL_RESOLUTION → notificationDispatchService.dispatcherAlerteResolue()
 *                        alerteBroadcastService.publierAlerteEtKpis(alerte, "RESOLUTION")
 *
 * Ce service ne gère PAS les notifications personnelles (bell icon, email outbox) —
 * c'est NotificationDispatchServiceImpl + NotificationPushService qui en sont responsables.
 */
@Service
public class AlerteBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(AlerteBroadcastService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final KpiService kpiService;

    public AlerteBroadcastService(
            SimpMessagingTemplate messagingTemplate,
            KpiService kpiService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.kpiService = kpiService;
    }

    /**
     * Publie l'alerte sur /topic/alertes ET recalcule/publie les KPI sur /topic/kpis.
     *
     * Les deux publications sont toujours effectuées ensemble — un client qui reçoit
     * l'alerte doit aussi avoir ses KPI à jour immédiatement.
     *
     * @param alerte         l'alerte concernée (déjà chargée par l'appelant)
     * @param evenement      "CREATION" ou "RESOLUTION" (cohérent avec AlerteMessage.evenement)
     * @param idPointMesure  ID du point de mesure (null acceptable pour RESOLUTION)
     * @param nomPointMesure nom du point de mesure (null acceptable pour RESOLUTION)
     */
    public void publierAlerteEtKpis(
            Alerte alerte,
            String evenement,
            Long idPointMesure,
            String nomPointMesure
    ) {
        // 1. Publier sur /topic/alertes
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

        // 2. Recalculer et publier les KPI
        var kpis = kpiService.getKpisGlobaux();
        messagingTemplate.convertAndSend("/topic/kpis",
                new KpiMessage(kpis.getAlertesActives(), kpis.getNbPointsEnAnomalie()));
        logger.info("[WS] KPI recalculés après {} alerte {} — alertesActives={}",
                evenement, alerte.getIdAlerte(), kpis.getAlertesActives());
    }
}
