package com.projet.notifications.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Wrapper autour du SDK officiel SendGrid (com.sendgrid:sendgrid-java).
 *
 * Responsabilités :
 *  - Construire et envoyer un email via l'API REST SendGrid v3 (POST /v3/mail/send).
 *  - Retourner un {@link SendGridResult} portant le code HTTP + message d'erreur.
 *  - Ne JAMAIS logger le contenu de l'email ni la clé API (même en debug).
 *
 * N'est PAS responsable de la logique de retry — c'est EmailWorkerService qui décide
 * quoi faire selon le code retourné (429 → laisser EN_ATTENTE, 4xx auth → ECHEC immédiat).
 */
@Service
public class SendGridEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    private static final String SENDGRID_ENDPOINT = "mail/send";
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    private final SendGrid sendGridClient;
    private final String senderEmail;

    public SendGridEmailService(
            SendGrid sendGridClient,
            @Value("${sendgrid.sender-email}") String senderEmail
    ) {
        this.sendGridClient = sendGridClient;
        this.senderEmail = senderEmail;
    }

    /**
     * Envoie un email de notification d'alerte.
     *
     * @param destinataireEmail adresse du superviseur (déjà validée en amont)
     * @param titre             sujet de l'email
     * @param contenu           corps de l'email (texte brut)
     * @return résultat portant le code HTTP SendGrid et le message d'erreur éventuel
     */
    public SendGridResult envoyerNotificationAlerte(String destinataireEmail, String titre, String contenu) {
        Mail mail = new Mail(
                new Email(senderEmail),
                titre,
                new Email(destinataireEmail),
                new Content(CONTENT_TYPE_TEXT, contenu)
        );

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint(SENDGRID_ENDPOINT);

        try {
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);
            int statusCode = response.getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("[SENDGRID] Email envoyé avec succès — code={}", statusCode);
                return SendGridResult.succes(statusCode);
            } else {
                // Tronquer le body pour les logs : on logue uniquement les 200 premiers chars
                String erreurTronquee = tronquer(response.getBody(), 200);
                logger.warn("[SENDGRID] Échec d'envoi — code={}, erreur={}", statusCode, erreurTronquee);
                return SendGridResult.echec(statusCode, buildMessageErreur(statusCode, response.getBody()));
            }

        } catch (IOException e) {
            // IOException réseau (timeout, DNS, etc.) — pas de code HTTP disponible
            logger.error("[SENDGRID] Erreur réseau lors de l'envoi — {}", e.getMessage());
            return SendGridResult.echec(0, "Erreur réseau : " + e.getMessage());
        }
    }

    /**
     * Implémentation du contrat EmailService existant (envoi du lien d'activation).
     * Remplace le StubEmailService.
     */
    @Override
    public void envoyerLienActivation(String email, String lienActivation) {
        String titre = "Activation de votre compte — Supervision Cabine de Peinture";
        String contenu = String.format(
                "Bonjour,%n%n" +
                "Votre compte a été créé. Cliquez sur le lien suivant pour l'activer :%n%n" +
                "%s%n%n" +
                "Ce lien expire dans 24 heures.%n%n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.",
                lienActivation
        );

        SendGridResult result = envoyerNotificationAlerte(email, titre, contenu);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi lien activation — id_envoi=N/A, code={}", result.statusCode());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Construit le message d'erreur stocké dans derniere_erreur.
     * Format : "HTTP {code} — {body tronqué à 500 chars}"
     * La clé API n'apparaît jamais dans le body SendGrid — pas de risque de fuite.
     */
    private String buildMessageErreur(int statusCode, String body) {
        return String.format("HTTP %d — %s", statusCode, tronquer(body, 500));
    }

    private String tronquer(String texte, int maxLen) {
        if (texte == null) return "";
        return texte.length() <= maxLen ? texte : texte.substring(0, maxLen) + "…";
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Valeur de retour portant le résultat d'un appel SendGrid.
     * Evite d'utiliser des exceptions pour le flux de contrôle normal.
     */
    public record SendGridResult(boolean isSucces, int statusCode, String messageErreur) {

        static SendGridResult succes(int statusCode) {
            return new SendGridResult(true, statusCode, null);
        }

        static SendGridResult echec(int statusCode, String messageErreur) {
            return new SendGridResult(false, statusCode, messageErreur);
        }

        /**
         * 429 Rate Limit : ne pas marquer ECHEC, laisser EN_ATTENTE pour retry.
         * L'erreur sera retentée au prochain cycle @Scheduled.
         */
        public boolean isRateLimit() {
            return statusCode == 429;
        }

        /**
         * 401/403 : clé API invalide ou sender non vérifié.
         * Un retry n'apportera rien sans intervention manuelle — ECHEC immédiat.
         */
        public boolean isEchecAuthConfiguration() {
            return statusCode == 401 || statusCode == 403;
        }
    }
}
