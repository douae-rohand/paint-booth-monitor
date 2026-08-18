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
import java.util.Map;

/**
 * Implémentation SendGrid de {@link EmailService}.
 *
 * Responsabilités (après refactoring) :
 *  1. Déléguer la construction HTML à {@link EmailTemplateBuilder} — aucun StringBuilder ici.
 *  2. Appeler l'API SendGrid (POST /v3/mail/send) via envoyerHtml() ou envoyerTexte().
 *  3. Traduire les codes HTTP → {@link EmailService.EmailResult} sémantique.
 *
 * Ne JAMAIS logger le contenu des emails ni la clé API.
 */
@Service
public class SendGridEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);
    private static final String SENDGRID_ENDPOINT = "mail/send";

    private final SendGrid sendGridClient;
    private final String senderEmail;
    private final String senderName;
    private final EmailTemplateBuilder templateBuilder;

    public SendGridEmailService(
            SendGrid sendGridClient,
            @Value("${sendgrid.sender-email}") String senderEmail,
            @Value("${sendgrid.sender-name}") String senderName,
            EmailTemplateBuilder templateBuilder
    ) {
        this.sendGridClient = sendGridClient;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.templateBuilder = templateBuilder;
    }

    // ── Emails transactionnels ────────────────────────────────────────────────

    @Override
    public EmailResult envoyerLienActivation(String email, String lienActivation) {
        String sujet = "Activation de votre compte - Supervision Cabine de Peinture";
        EmailResult r = envoyerHtml(email, sujet,
                templateBuilder.activation(sujet, lienActivation));
        if (!r.isSucces()) logger.error("[SENDGRID] Échec lien activation - statut={}", r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerLienReinitialisation(String email, String lienReinitialisation) {
        String sujet = "Réinitialisation de votre mot de passe - Supervision Cabine de Peinture";
        EmailResult r = envoyerHtml(email, sujet,
                templateBuilder.reinitialisation(sujet, lienReinitialisation));
        if (!r.isSucces()) logger.error("[SENDGRID] Échec lien réinitialisation - statut={}", r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerNotificationDesactivation(String email, String prenom) {
        String sujet = "Désactivation de votre compte - Supervision Cabine de Peinture";
        EmailResult r = envoyerHtml(email, sujet,
                templateBuilder.desactivation(sujet, prenom));
        if (!r.isSucces()) logger.error("[SENDGRID] Échec désactivation {} - statut={}", email, r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerEmailBienvenue(String email, String prenom) {
        String sujet = "Bienvenue sur Supervision Cabine de Peinture !";
        EmailResult r = envoyerHtml(email, sujet,
                templateBuilder.bienvenue(sujet, prenom));
        if (!r.isSucces()) logger.error("[SENDGRID] Échec bienvenue {} - statut={}", email, r.statut());
        return r;
    }

    // ── Emails outbox ─────────────────────────────────────────────────────────

    @Override
    public EmailResult envoyerNotificationAlerte(
            String destinataireEmail, String titre, Map<String, Object> donneesEvenement
    ) {
        EmailResult r = envoyerHtml(destinataireEmail, titre,
                templateBuilder.alerte(titre, donneesEvenement));
        if (!r.isSucces())
            logger.error("[SENDGRID] Échec alerte email {} - statut={}", destinataireEmail, r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerNotificationCompteActive(
            String destinataireEmail, String titre, Map<String, Object> donneesEvenement
    ) {
        EmailResult r = envoyerHtml(destinataireEmail, titre,
                templateBuilder.compteActive(titre, donneesEvenement));
        if (!r.isSucces())
            logger.error("[SENDGRID] Échec compte activé {} - statut={}", destinataireEmail, r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerNotificationSeuilModifie(
            String destinataireEmail, String titre, Map<String, Object> donneesEvenement
    ) {
        EmailResult r = envoyerHtml(destinataireEmail, titre,
                templateBuilder.seuilModifie(titre, donneesEvenement));
        if (!r.isSucces())
            logger.error("[SENDGRID] Échec seuil modifié {} - statut={}", destinataireEmail, r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerNotificationRapportGenere(
            String destinataireEmail, String titre, Map<String, Object> donneesEvenement
    ) {
        EmailResult r = envoyerHtml(destinataireEmail, titre,
                templateBuilder.rapportGenere(titre, donneesEvenement));
        if (!r.isSucces())
            logger.error("[SENDGRID] Échec rapport généré {} - statut={}", destinataireEmail, r.statut());
        return r;
    }

    @Override
    public EmailResult envoyerNotification(String destinataireEmail, String titre, String contenu) {
        // Fallback texte brut — uniquement si donnees_evenement est null (cas exceptionnel)
        Mail mail = new Mail(
                new Email(senderEmail, senderName), titre,
                new Email(destinataireEmail),
                new Content("text/plain", contenu)
        );
        return envoyer(mail);
    }

    // ── Envoi HTTP (privé) ────────────────────────────────────────────────────

    private EmailResult envoyerHtml(String destinataireEmail, String sujet, String htmlBody) {
        Mail mail = new Mail(
                new Email(senderEmail, senderName), sujet,
                new Email(destinataireEmail),
                new Content("text/html", htmlBody)
        );
        return envoyer(mail);
    }

    private EmailResult envoyer(Mail mail) {
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint(SENDGRID_ENDPOINT);
        try {
            request.setBody(mail.build());
            return traduireReponse(sendGridClient.api(request));
        } catch (IOException e) {
            logger.error("[SENDGRID] Erreur réseau - {}", e.getMessage());
            return EmailResult.echecTemporaire("Erreur réseau : " + e.getMessage());
        }
    }

    // ── Traduction HTTP → EmailResult ─────────────────────────────────────────

    private EmailResult traduireReponse(Response response) {
        int code = response.getStatusCode();
        if (code >= 200 && code < 300) {
            logger.info("[SENDGRID] Email envoyé - code={}", code);
            return EmailResult.succes();
        }
        String err = String.format("HTTP %d - %s", code, tronquer(response.getBody(), 500));
        logger.warn("[SENDGRID] Échec - code={}, erreur={}", code, tronquer(response.getBody(), 200));
        if (code == 429)              return EmailResult.echecTemporaire(err);
        if (code >= 500)              return EmailResult.echecTemporaire(err);
        if (code == 401 || code == 403) return EmailResult.echecDefinitif(err);
        return EmailResult.echecDefinitif(err);
    }

    private String tronquer(String texte, int maxLen) {
        if (texte == null) return "";
        return texte.length() <= maxLen ? texte : texte.substring(0, maxLen) + "…";
    }
}
