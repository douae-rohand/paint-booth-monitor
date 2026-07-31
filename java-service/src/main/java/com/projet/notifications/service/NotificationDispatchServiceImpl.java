package com.projet.notifications.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.gateway.dto.AlerteMessage;
import com.projet.gateway.dto.KpiMessage;
import com.projet.kpis.service.KpiService;
import com.projet.measures.model.Mesure;
import com.projet.measures.repository.MesureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implémentation du service de dispatch des notifications d'alerte.
 * 
 * Cette implémentation publie les alertes sur WebSocket pour le dashboard temps réel.
 * L'implémentation réelle (email, WhatsApp, push) sera branchée ultérieurement
 * en cohérence avec CDC section 7.
 */
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchServiceImpl.class);

    private final AlerteRepository alerteRepository;
    private final KpiService kpiService;
    private final MesureRepository mesureRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDispatchServiceImpl(
            AlerteRepository alerteRepository,
            KpiService kpiService,
            MesureRepository mesureRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.alerteRepository = alerteRepository;
        this.kpiService = kpiService;
        this.mesureRepository = mesureRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void dispatcherAlerte(UUID idAlerte) {
        logger.info("Alerte {} reçue via NOTIFY, publication WebSocket en cours", idAlerte);
        
        // Récupérer l'alerte avec ses détails
        alerteRepository.findById(idAlerte).ifPresent(alerte -> {
            // Publier sur /topic/alertes
            publishAlerteWebSocket(alerte);
            
            // Recalculer et publier les KPIs globaux
            publishKpisWebSocket();
        });
    }

    private void publishAlerteWebSocket(Alerte alerte) {
        // Récupérer la mesure avec son point de mesure chargé (JOIN FETCH) pour éviter LazyInitializationException
        var mesure = mesureRepository.findByIdWithPointMesure(alerte.getIdMesure());
        if (mesure == null) {
            logger.warn("Mesure {} non trouvée pour l'alerte {}", alerte.getIdMesure(), alerte.getIdAlerte());
            return;
        }

        var pointMesure = mesure.getPointMesure();
        if (pointMesure == null) {
            logger.warn("Point de mesure non trouvé pour la mesure {}", mesure.getIdMesure());
            return;
        }

        AlerteMessage message = new AlerteMessage(
                alerte.getIdAlerte(),
                pointMesure.getId(),
                pointMesure.getNom(),
                alerte.getMetrique(),
                alerte.getTypeAlerte(),
                alerte.getSeverite(),
                alerte.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/alertes", message);
        logger.info("Alerte publiée sur /topic/alertes: {}", alerte.getIdAlerte());
    }

    private void publishKpisWebSocket() {
        var kpis = kpiService.getKpisGlobaux();
        
        KpiMessage message = new KpiMessage(
                kpis.getAlertesActives(),
                kpis.getNbPointsEnAnomalie()
        );

        messagingTemplate.convertAndSend("/topic/kpis", message);
        logger.info("KPIs publiés sur /topic/kpis: alertesActives={}, nbPointsEnAnomalie={}", 
                message.getAlertesActives(), message.getNbPointsEnAnomalie());
    }
}
