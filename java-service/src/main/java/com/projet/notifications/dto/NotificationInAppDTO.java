package com.projet.notifications.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projet.notifications.model.enums.TypeEvenement;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de notification IN_APP envoyé :
 *  - via WebSocket (push immédiat sur /user/queue/notifications)
 *  - via REST (GET /api/notifications, panel bell icon)
 *
 * Le champ contenu est un texte lisible généré à la volée dans NotificationInAppService.toDTO()
 * à partir de donnees_evenement (JSONB) — il n'est PAS lu depuis la base de données.
 * Le frontend est un afficheur pur : il n'interprète pas le typeEvenement pour formater.
 */
public record NotificationInAppDTO(

        /** ID de l'EnvoiNotification (utilisé pour PATCH .../lu) */
        UUID idEnvoi,

        /** ID de la Notification parente */
        UUID idNotification,

        TypeEvenement typeEvenement,

        /** Titre court (stocké en base, généré une fois à la création) */
        String titre,

        /** Texte lisible généré à la volée depuis donneesEvenement — pas stocké en base */
        String contenu,

        boolean lu,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateCreation,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateLecture
) {}
