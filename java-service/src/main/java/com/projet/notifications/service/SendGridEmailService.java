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
 * Implémentation SendGrid de {@link EmailService}.
 *
 * Responsabilités :
 *  - Construire et envoyer un email via l'API REST SendGrid v3 (POST /v3/mail/send).
 *  - Traduire les codes HTTP SendGrid en {@link EmailService.EmailResult} sémantique :
 *      2xx            → SUCCES
 *      429            → ECHEC_TEMPORAIRE (rate limit — retry au prochain cycle)
 *      5xx            → ECHEC_TEMPORAIRE (erreur serveur transitoire)
 *      401 / 403      → ECHEC_DEFINITIF  (auth/config — intervention manuelle requise)
 *      autres 4xx     → ECHEC_DEFINITIF  (adresse invalide, contenu refusé, etc.)
 *      IOException    → ECHEC_TEMPORAIRE (erreur réseau transitoire)
 *  - Ne JAMAIS exposer de type interne SendGrid vers les appelants.
 *  - Ne JAMAIS logger le contenu de l'email ni la clé API.
 */
@Service
public class SendGridEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    private static final String SENDGRID_ENDPOINT = "mail/send";
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    private final SendGrid sendGridClient;
    private final String senderEmail;
    private final String senderName;
    private final String frontendUrl;

    public SendGridEmailService(
            SendGrid sendGridClient,
            @Value("${sendgrid.sender-email}") String senderEmail,
            @Value("${sendgrid.sender-name}") String senderName,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.sendGridClient = sendGridClient;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.frontendUrl = frontendUrl;
    }

    // ── Contrat EmailService ──────────────────────────────────────────────────

    @Override
    public EmailResult envoyerNotification(String destinataireEmail, String titre, String contenu) {
        Mail mail = new Mail(
                new Email(senderEmail, senderName),
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
            return traduireReponse(response);

        } catch (IOException e) {
            // Erreur réseau (timeout, DNS…) — transitoire, retry pertinent
            logger.error("[SENDGRID] Erreur réseau - {}", e.getMessage());
            return EmailResult.echecTemporaire("Erreur réseau : " + e.getMessage());
        }
    }

    private EmailResult envoyerEmailHtml(String destinataireEmail, String titre, String htmlBody) {
        Mail mail = new Mail(
                new Email(senderEmail, senderName),
                titre,
                new Email(destinataireEmail),
                new Content("text/html", htmlBody)
        );

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint(SENDGRID_ENDPOINT);

        try {
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);
            return traduireReponse(response);

        } catch (IOException e) {
            logger.error("[SENDGRID] Erreur réseau lors de l'envoi HTML - {}", e.getMessage());
            return EmailResult.echecTemporaire("Erreur réseau : " + e.getMessage());
        }
    }

    @Override
    public EmailResult envoyerLienActivation(String email, String lienActivation) {
        String titre = "Activation de votre compte - Supervision Cabine de Peinture";
        String message = "Bonjour,\n\nVotre compte a été créé. Pour commencer à l'utiliser, veuillez activer votre compte en définissant votre mot de passe à l'aide du bouton ci-dessous :";
        
        String html = construireTemplateEmail(
                "Activation de compte",
                message,
                "Activer mon compte",
                lienActivation,
                "Ce lien expire dans 24 heures."
        );

        EmailResult result = envoyerEmailHtml(email, titre, html);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi lien activation - statut={}", result.statut());
        }
        return result;
    }

    @Override
    public EmailResult envoyerLienReinitialisation(String email, String lienReinitialisation) {
        String titre = "Réinitialisation de votre mot de passe - Supervision Cabine de Peinture";
        String message = "Bonjour,\n\nUne demande de réinitialisation de mot de passe a été effectuée pour votre compte. Veuillez cliquer sur le bouton ci-dessous pour définir un nouveau mot de passe :";
        
        String html = construireTemplateEmail(
                "Réinitialisation du mot de passe",
                message,
                "Définir un nouveau mot de passe",
                lienReinitialisation,
                "Ce lien expire dans 15 minutes."
        );

        EmailResult result = envoyerEmailHtml(email, titre, html);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi lien réinitialisation - statut={}", result.statut());
        }
        return result;
    }

    @Override
    public EmailResult envoyerNotificationDesactivation(String email, String prenom) {
        String titre = "Désactivation de votre compte - Supervision Cabine de Peinture";
        String message = String.format(
                "Bonjour %s,\n\nNous vous informons que votre compte de supervision a été désactivé par un administrateur.\n" +
                "Vous ne pouvez plus vous connecter à la plateforme.\n\n" +
                "Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez le réactiver, veuillez contacter votre administrateur principal.",
                prenom
        );

        String html = construireTemplateEmail(
                "Compte désactivé",
                message,
                null,
                null,
                null
        );

        EmailResult result = envoyerEmailHtml(email, titre, html);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi notification désactivation pour {} - statut={}", email, result.statut());
        }
        return result;
    }

    @Override
    public EmailResult envoyerEmailBienvenue(String email, String prenom) {
        String titre = "Bienvenue sur Supervision Cabine de Peinture !";
        String message = String.format(
                "Bonjour %s,\n\nVotre compte a été activé avec succès et votre mot de passe a été configuré.\n" +
                "Vous pouvez maintenant vous connecter à la plateforme pour suivre de près l'état des cabines de peinture.",
                prenom
        );

        String urlLogin = frontendUrl + "/login";
        String html = construireTemplateEmail(
                "Compte activé !",
                message,
                "Se connecter à la plateforme",
                urlLogin,
                null
        );

        EmailResult result = envoyerEmailHtml(email, titre, html);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi email bienvenue pour {} - statut={}", email, result.statut());
        }
        return result;
    }

    @Override
    public EmailResult envoyerNotificationAlerte(
            String destinataireEmail,
            String sujet,
            String metrique,
            String typeAlerte,
            String severite,
            String dateHeure,
            String idAlerte,
            String urlTableauBord,
            String emplacement,
            String pointMesureNom
    ) {
        String severiteDisplay = switch (severite) {
            case "CRITIQUE" -> "🔴 CRITIQUE";
            case "MOYENNE"  -> "🟡 MOYENNE";
            default         -> "🔵 " + severite;
        };

        String emplacementLabel = emplacement != null ? emplacement : "l'équipement";
        if ("CABINE".equalsIgnoreCase(emplacement)) {
            emplacementLabel = "la cabine de peinture";
        } else if ("ETUVE".equalsIgnoreCase(emplacement)) {
            emplacementLabel = "l'étuve";
        }

        String message = String.format(
                "Une anomalie a été détectée sur %s (<strong>%s</strong>). Voici les détails :<br><br>" +
                "<table style='border-collapse:collapse; width:100%%; font-size:14px; color:#475569;'>" +
                "<tr><td style='padding:6px 0; font-weight:600; width:120px;'>Métrique</td><td style='padding:6px 0;'>%s</td></tr>" +
                "<tr style='background:#f8fafc;'><td style='padding:6px 0; font-weight:600;'>Type</td><td style='padding:6px 0;'>%s</td></tr>" +
                "<tr><td style='padding:6px 0; font-weight:600;'>Sévérité</td><td style='padding:6px 0;'>%s</td></tr>" +
                "<tr style='background:#f8fafc;'><td style='padding:6px 0; font-weight:600;'>Date / Heure</td><td style='padding:6px 0;'>%s</td></tr>" +
                "<tr><td style='padding:6px 0; font-weight:600;'>ID alerte</td><td style='padding:6px 4px; font-family:monospace; font-size:12px; color:#94a3b8;'>%s</td></tr>" +
                "</table>",
                emplacementLabel, pointMesureNom, metrique, typeAlerte, severiteDisplay, dateHeure, idAlerte
        );

        String html = construireTemplateEmail(
                "Alerte détectée",
                message,
                "Consulter le tableau de bord",
                urlTableauBord,
                null
        );

        EmailResult result = envoyerEmailHtml(destinataireEmail, sujet, html);
        if (!result.isSucces()) {
            logger.error("[SENDGRID] Échec envoi alerte email à {} - statut={}", destinataireEmail, result.statut());
        }
        return result;
    }

    private String construireTemplateEmail(String titre, String message, String texteBouton, String urlBouton, String mentionValidite) {
        boolean hasButton = urlBouton != null && !urlBouton.trim().isEmpty() && texteBouton != null && !texteBouton.trim().isEmpty();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("  <meta charset='utf-8'>");
        sb.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("  <title>").append(titre).append("</title>");
        sb.append("  <style>");
        sb.append("    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }");
        sb.append("    .wrapper { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0; overflow: hidden; }");
        sb.append("    .header { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); padding: 32px 24px; text-align: center; }");
        sb.append("    .header-title { color: #f8fafc; font-size: 20px; font-weight: 700; letter-spacing: 0.5px; margin: 0; text-transform: uppercase; }");
        sb.append("    .content { padding: 40px 32px; }");
        sb.append("    .title { color: #0f172a; font-size: 24px; font-weight: 700; margin: 0 0 20px 0; }");
        sb.append("    .text { color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0; }");
        sb.append("    .button-container { text-align: center; margin: 30px 0; }");
        sb.append("    .btn { display: inline-block; background-color: #E88A2A; color: #ffffff !important; font-size: 16px; font-weight: 600; text-decoration: none; padding: 14px 28px; border-radius: 8px; box-shadow: 0 4px 12px rgba(232, 138, 42, 0.25); transition: background-color 0.2s ease; }");
        sb.append("    .fallback-container { background-color: #f1f5f9; border-radius: 8px; padding: 16px; margin: 24px 0; word-break: break-all; border-left: 4px solid #cbd5e1; }");
        sb.append("    .fallback-title { font-size: 12px; font-weight: 600; color: #64748b; margin: 0 0 8px 0; text-transform: uppercase; }");
        sb.append("    .fallback-link { font-size: 14px; color: #0284c7; text-decoration: none; }");
        sb.append("    .badge-validity { display: inline-block; background-color: #fef3c7; color: #d97706; font-size: 13px; font-weight: 600; padding: 6px 12px; border-radius: 9999px; margin-bottom: 24px; }");
        sb.append("    .footer { background-color: #f8fafc; padding: 24px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 13px; color: #64748b; }");
        sb.append("  </style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("  <div class='wrapper'>");
        sb.append("    <div class='header'>");
        sb.append("      <h1 class='header-title'>Supervision Cabine de Peinture</h1>");
        sb.append("    </div>");
        sb.append("    <div class='content'>");
        sb.append("      <h2 class='title'>").append(titre).append("</h2>");
        sb.append("      <p class='text'>").append(message.replace("\n", "<br>")).append("</p>");
        
        if (hasButton) {
            sb.append("      <div class='button-container'>");
            sb.append("        <a href='").append(urlBouton).append("' class='btn' target='_blank'>").append(texteBouton).append("</a>");
            sb.append("      </div>");
            sb.append("      <div class='fallback-container'>");
            sb.append("        <p class='fallback-title'>Si le bouton ne fonctionne pas, copiez ce lien :</p>");
            sb.append("        <a href='").append(urlBouton).append("' class='fallback-link' target='_blank'>").append(urlBouton).append("</a>");
            sb.append("      </div>");
        }
        
        if (mentionValidite != null && !mentionValidite.trim().isEmpty()) {
            sb.append("      <div class='badge-validity'>").append(mentionValidite).append("</div>");
        }
        
        sb.append("    </div>");
        sb.append("    <div class='footer'>");
        sb.append("      © Supervision Cabine de Peinture | Tous droits réservés");
        sb.append("    </div>");
        sb.append("  </div>");
        sb.append("</body>");
        sb.append("</html>");
        
        return sb.toString();
    }

    // ── Traduction HTTP → EmailResult (privé, jamais exposé) ─────────────────

    /**
     * Traduit la réponse brute SendGrid en {@link EmailResult} sémantique.
     * Toute la connaissance des codes HTTP SendGrid est encapsulée ici.
     */
    private EmailResult traduireReponse(Response response) {
        int code = response.getStatusCode();

        if (code >= 200 && code < 300) {
            logger.info("[SENDGRID] Email envoyé - code={}", code);
            return EmailResult.succes();
        }

        String messageErreur = buildMessageErreur(code, response.getBody());
        logger.warn("[SENDGRID] Échec d'envoi - code={}, erreur={}", code, tronquer(response.getBody(), 200));

        // 429 Rate Limit : erreur transitoire — le prochain cycle @Scheduled retentera
        if (code == 429) {
            return EmailResult.echecTemporaire(messageErreur);
        }

        // 5xx Erreur serveur SendGrid : transitoire
        if (code >= 500) {
            return EmailResult.echecTemporaire(messageErreur);
        }

        // 401/403 : clé API invalide ou sender non vérifié — aucun retry utile
        if (code == 401 || code == 403) {
            return EmailResult.echecDefinitif(messageErreur);
        }

        // Autres 4xx (400 bad request, 413 payload too large, etc.) : définitif
        return EmailResult.echecDefinitif(messageErreur);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Format : "HTTP {code} — {body tronqué à 500 chars}". La clé API n'apparaît jamais dans le body SendGrid. */
    private String buildMessageErreur(int statusCode, String body) {
        return String.format("HTTP %d - %s", statusCode, tronquer(body, 500));
    }

    private String tronquer(String texte, int maxLen) {
        if (texte == null) return "";
        return texte.length() <= maxLen ? texte : texte.substring(0, maxLen) + "…";
    }
}
