package com.projet.notifications.service;

import com.projet.notifications.dto.NotificationInAppDTO;
import com.projet.notifications.model.enums.TypeEvenement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service technique de push WebSocket pour les notifications IN_APP personnelles.
 *
 * Responsabilité unique : construire le DTO et pousser le message via STOMP
 * sur /user/queue/notifications pour un destinataire précis.
 *
 * Pas d'interface — un seul mécanisme de push existe (WebSocket STOMP),
 * contrairement à EmailService qui a plusieurs fournisseurs possibles.
 *
 * Appelé par NotificationDispatchServiceImpl après la persistance de EnvoiNotification.
 * NotificationDispatchServiceImpl ne connaît plus SimpMessagingTemplate pour la partie IN_APP.
 *
 * Note : la partie broadcast globale (/topic/alertes, /topic/kpis) reste dans
 * NotificationDispatchServiceImpl — elle concerne tous les clients connectés,
 * pas un destinataire précis.
 */
@Service
public class NotificationPushService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPushService.class);
    private static final String USER_NOTIFICATION_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationFormatter formatter;

    public NotificationPushService(
            SimpMessagingTemplate messagingTemplate,
            NotificationFormatter formatter
    ) {
        this.messagingTemplate = messagingTemplate;
        this.formatter = formatter;
    }

    /**
     * Pousse une notification IN_APP vers un destinataire précis via WebSocket STOMP.
     *
     * Le contenu textuel est formaté ici via {@link NotificationFormatter} —
     * même logique que dans {@link NotificationInAppService#toDTO()} pour le panel bell.
     * Garantit l'identité du texte entre le toast initial et la relecture de l'historique.
     *
     * Si l'utilisateur n'est pas connecté : message perdu (normal pour du push direct).
     * La ligne EnvoiNotification est déjà persistée en base — consultable via REST.
     *
     * @param idEnvoi         UUID de l'EnvoiNotification (déjà sauvegardé)
     * @param idNotification  UUID de la Notification parente
     * @param idDestinataire  UUID du Superviseur destinataire (= Principal STOMP)
     * @param typeEvenement   type de l'événement
     * @param titre           titre court (déjà construit par NotificationDispatchServiceImpl)
     * @param donneesEvenement données brutes JSONB
     * @param dateCreation    date de création de la Notification
     */
    public void pousser(
            UUID idEnvoi,
            UUID idNotification,
            UUID idDestinataire,
            TypeEvenement typeEvenement,
            String titre,
            Map<String, Object> donneesEvenement,
            LocalDateTime dateCreation
    ) {
        String contenu = formatter.formaterContenuAffichage(typeEvenement, donneesEvenement);

        NotificationInAppDTO dto = new NotificationInAppDTO(
                idEnvoi,
                idNotification,
                typeEvenement,
                titre,
                contenu,
                false,
                dateCreation,
                null
        );

        messagingTemplate.convertAndSendToUser(
                idDestinataire.toString(),
                USER_NOTIFICATION_QUEUE,
                dto
        );

        logger.info("[PUSH IN_APP] Notification poussée à {} ({})", idDestinataire, typeEvenement);
    }
}
