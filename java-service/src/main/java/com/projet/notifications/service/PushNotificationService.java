package com.projet.notifications.service;

import com.projet.notifications.model.AbonnementPushNavigateur;
import com.projet.notifications.repository.AbonnementPushNavigateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;
import java.util.UUID;

/**
 * Service d'envoi de notifications Web Push (VAPID) pour le canal PUSH.
 *
 * Responsabilité unique : envoyer des notifications push aux navigateurs via le standard Web Push.
 * Utilise la bibliothèque nl.martijndwars:web-push avec les clés VAPID configurées.
 *
 * Pattern identique à NotificationPushService (IN_APP) :
 * - Ne fait jamais échouer l'opération métier appelante
 * - Les erreurs réseau sont loggées mais ne propagent pas d'exception
 * - Les abonnements expirés (404/410) sont nettoyés automatiquement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final AbonnementPushNavigateurRepository abonnementPushNavigateurRepository;

    @Value("${vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${vapid.subject:}")
    private String vapidSubject;

    static {
        // BouncyCastle requis pour le chiffrement Web Push
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Envoie une notification Web Push à tous les abonnements actifs d'un superviseur.
     *
     * @param idSuperviseur ID du superviseur destinataire
     * @param titre Titre de la notification
     * @param corps Corps de la notification
     * @param urlCible URL optionnelle à ouvrir lors du clic
     */
    public void envoyer(UUID idSuperviseur, String titre, String corps, String urlCible) {
        // Clés VAPID non configurées → canal PUSH désactivé, on skippe silencieusement
        if (vapidPublicKey == null || vapidPublicKey.isBlank()
                || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.debug("[PUSH] Clés VAPID non configurées - notifications PUSH désactivées");
            return;
        }

        List<AbonnementPushNavigateur> abonnements = abonnementPushNavigateurRepository
                .findBySuperviseur_IdSuperviseur(idSuperviseur);

        if (abonnements.isEmpty()) {
            log.debug("[PUSH] Aucun abonnement actif pour superviseur {}", idSuperviseur);
            return;
        }

        PushService pushService;
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (Exception e) {
            log.error("[PUSH] Impossible d'initialiser PushService (clés VAPID invalides ?) : {}", e.getMessage());
            return;
        }

        for (AbonnementPushNavigateur abonnement : abonnements) {
            try {
                Notification notification = new Notification(
                        abonnement.getEndpoint(),
                        abonnement.getCleP256dh(),
                        abonnement.getCleAuth(),
                        buildPayload(titre, corps, urlCible)
                );

                pushService.send(notification);
                log.info("[PUSH] Notification envoyée à {} (superviseur {})", abonnement.getEndpoint(), idSuperviseur);

            } catch (Exception e) {
                // Vérifier si l'erreur est due à un abonnement expiré (404 ou 410)
                if (isSubscriptionExpired(e)) {
                    log.warn("[PUSH] Abonnement expiré {}, suppression automatique (superviseur {})",
                            abonnement.getEndpoint(), idSuperviseur);
                    abonnementPushNavigateurRepository.delete(abonnement);
                } else {
                    // Autre erreur réseau : logger mais ne pas propager
                    log.error("[PUSH] Échec envoi à {} (superviseur {}) : {}",
                            abonnement.getEndpoint(), idSuperviseur, e.getMessage());
                }
            }
        }
    }

    /**
     * Construit le payload JSON pour la notification Web Push.
     *
     * @param titre Titre
     * @param corps Corps
     * @param urlCible URL optionnelle
     * @return Payload JSON en bytes
     */
    private byte[] buildPayload(String titre, String corps, String urlCible) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"title\":\"").append(escapeJson(titre)).append("\",");
        json.append("\"body\":\"").append(escapeJson(corps)).append("\"");

        if (urlCible != null && !urlCible.isBlank()) {
            json.append(",\"data\":{\"url\":\"").append(escapeJson(urlCible)).append("\"}");
        }

        json.append("}");
        return json.toString().getBytes();
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Détermine si une exception indique un abonnement expiré (404 ou 410).
     */
    private boolean isSubscriptionExpired(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        return message.contains("404") || message.contains("410");
    }
}
