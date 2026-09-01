package com.projet.notifications.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Constructeur de templates HTML pour les emails transactionnels.
 *
 * Responsabilité unique : produire le HTML final pour chaque type d'email.
 * Tous les templates partagent la même structure visuelle (en-tête, contenu, pied de page)
 * via {@link #construireStructure(String, String, String, String, String)}.
 *
 * SendGridEmailService délègue entièrement la construction HTML ici — il ne contient
 * plus aucun StringBuilder ni logique de mise en forme.
 *
 * Règle : le titre est TOUJOURS fourni par l'appelant — jamais généré en interne.
 * Cette classe ne fait que mettre en forme, pas décider du contenu sémantique.
 */
@Component
public class EmailTemplateBuilder {

    private final String frontendUrl;

    public EmailTemplateBuilder(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    // ── Templates transactionnels (hors outbox) ───────────────────────────────

    /**
     * Email d'activation de compte — envoyé au nouveau superviseur.
     * Bouton CTA : lien d'activation (expire dans 24h).
     */
    public String activation(String titre, String lienActivation) {
        String message = "Bonjour,<br><br>"
                + "Votre compte a été créé. Pour commencer à l'utiliser, "
                + "veuillez activer votre compte en définissant votre mot de passe :";
        return construireStructure(titre, message,
                "Activer mon compte", lienActivation, "Ce lien expire dans 24 heures.");
    }

    /**
     * Email de réinitialisation du mot de passe.
     * Bouton CTA : lien de réinitialisation (expire dans 15min).
     */
    public String reinitialisation(String titre, String lienReinitialisation) {
        String message = "Bonjour,<br><br>"
                + "Une demande de réinitialisation de mot de passe a été effectuée pour votre compte. "
                + "Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :";
        return construireStructure(titre, message,
                "Définir un nouveau mot de passe", lienReinitialisation, "Ce lien expire dans 15 minutes.");
    }

    /**
     * Email de notification de désactivation de compte.
     * Pas de bouton CTA — email purement informatif.
     */
    public String desactivation(String titre, String prenom) {
        String message = String.format(
                "Bonjour %s,<br><br>"
                + "Nous vous informons que votre compte de supervision a été désactivé par un administrateur.<br>"
                + "Vous ne pouvez plus vous connecter à la plateforme.<br><br>"
                + "Si vous pensez qu'il s'agit d'une erreur, veuillez contacter votre administrateur principal.",
                prenom);
        return construireStructure(titre, message, null, null, null);
    }

    /**
     * Email de bienvenue envoyé au superviseur après activation réussie.
     * Bouton CTA : lien vers la page de connexion.
     */
    public String bienvenue(String titre, String prenom) {
        String message = String.format(
                "Bonjour %s,<br><br>"
                + "Votre compte a été activé avec succès et votre mot de passe a été configuré.<br>"
                + "Vous pouvez maintenant vous connecter à la plateforme pour suivre l'état des cabines de peinture.",
                prenom);
        return construireStructure(titre, message,
                "Se connecter à la plateforme", frontendUrl + "/login", null);
    }

    // ── Templates outbox (EmailWorkerService) ─────────────────────────────────

    /**
     * Template HTML pour ALERTE_CREE / ALERTE_RESOLU.
     * Le message d'introduction diffère selon le type :
     *   ALERTE_CREE  → "Une anomalie a été détectée..."
     *   ALERTE_RESOLU → "L'anomalie a été résolue..."
     * Le titre est lu depuis Notification.titre — jamais généré ici.
     *
     * @param typeEvenement  "ALERTE_CREE" ou "ALERTE_RESOLU"
     * @param donneesEvenement map JSONB contenant : idAlerte, metrique, typeAlerte, severite,
     *                         nomPointMesure, typeEmplacement, dateEvenement, urlTableauBord (optionnel)
     */
    public String alerte(String titre, String typeEvenement, Map<String, Object> donneesEvenement) {
        String metrique       = str(donneesEvenement, "metrique");
        String typeAlerte     = str(donneesEvenement, "typeAlerte");
        String severite       = str(donneesEvenement, "severite");
        String dateHeure      = str(donneesEvenement, "dateEvenement");
        String idAlerte       = str(donneesEvenement, "idAlerte");
        String emplacement    = str(donneesEvenement, "typeEmplacement");
        String pointMesureNom = str(donneesEvenement, "nomPointMesure");

        String urlRaw = str(donneesEvenement, "urlTableauBord");
        String urlTableauBord = urlRaw.isBlank() ? frontendUrl + "/alertes" : urlRaw;

        String severiteDisplay = switch (severite) {
            case "CRITIQUE" -> "CRITIQUE";
            case "MOYENNE"  -> "MOYENNE";
            default         -> severite;
        };

        String emplacementLabel;
        if ("CABINE".equalsIgnoreCase(emplacement)) {
            emplacementLabel = "la cabine";
        } else if ("ETUVE".equalsIgnoreCase(emplacement)) {
            emplacementLabel = "l'étuve";
        } else {
            emplacementLabel = emplacement != null ? emplacement : "l'équipement";
        }

        boolean estResolution = "ALERTE_RESOLU".equals(typeEvenement);

        String intro = estResolution
                ? String.format("L'anomalie sur %s (<strong>%s</strong>) a été résolue. Voici les détails :<br><br>",
                        emplacementLabel, pointMesureNom)
                : String.format("Une anomalie a été détectée sur %s (<strong>%s</strong>). Voici les détails :<br><br>",
                        emplacementLabel, pointMesureNom);

        String dateLabel = estResolution ? "Date résolution" : "Date / Heure";

        String message = intro
                + "<table style='border-collapse:collapse;width:100%;font-size:14px;color:#475569;'>"
                + "<tr><td style='padding:6px 0;font-weight:600;width:120px;'>Métrique</td>"
                +     "<td style='padding:6px 0;'>" + metrique + "</td></tr>"
                + "<tr style='background:#f8fafc;'><td style='padding:6px 0;font-weight:600;'>Type</td>"
                +     "<td style='padding:6px 0;'>" + typeAlerte + "</td></tr>"
                + "<tr><td style='padding:6px 0;font-weight:600;'>Sévérité</td>"
                +     "<td style='padding:6px 0;'>" + severiteDisplay + "</td></tr>"
                + "<tr style='background:#f8fafc;'><td style='padding:6px 0;font-weight:600;'>" + dateLabel + "</td>"
                +     "<td style='padding:6px 0;'>" + dateHeure + "</td></tr>"
                + "<tr><td style='padding:6px 0;font-weight:600;'>ID alerte</td>"
                +     "<td style='padding:6px 4px;font-family:monospace;font-size:12px;color:#94a3b8;'>" + idAlerte + "</td></tr>"
                + "</table>";

        String btnTexte = estResolution ? "Voir l'historique des alertes" : "Consulter le tableau de bord";
        return construireStructure(titre, message, btnTexte, urlTableauBord, null);
    }

    /**
     * Template HTML pour COMPTE_ACTIVEE — email informatif aux Admins.
     * Bouton CTA : lien vers la liste des superviseurs (Admin).
     * Le titre est lu depuis Notification.titre — jamais généré ici.
     */
    public String compteActive(String titre, Map<String, Object> donneesEvenement) {
        String prenom = str(donneesEvenement, "prenomSuperviseur");
        String nom = str(donneesEvenement, "nomSuperviseur");
        String dateActivation = str(donneesEvenement, "dateActivation");

        String message = String.format(
                "Le compte superviseur de <strong>%s %s</strong> vient d'être activé le %s.<br><br>"
                + "Ce superviseur peut maintenant accéder à la plateforme de supervision "
                + "et consulter les données des cabines de peinture.",
                prenom, nom, dateActivation);

        String urlSuperviseurs = frontendUrl + "/superviseurs";
        return construireStructure(titre, message, "Voir les superviseurs", urlSuperviseurs, null);
    }

    /**
     * Template HTML pour CONFIG_SEUILS_MODIFIE — email aux Superviseurs.
     * Bouton CTA : lien vers le dashboard (page seuils).
     * Le titre est lu depuis Notification.titre — jamais généré ici.
     */
    public String seuilModifie(String titre, Map<String, Object> donneesEvenement) {
        String nomPointMesure = str(donneesEvenement, "nomPointMesure");
        String metrique = str(donneesEvenement, "metrique");
        String typeModification = str(donneesEvenement, "typeModification");
        String dateModification = str(donneesEvenement, "dateModification");

        String typeLabel = "SEUIL_ABSOLU".equals(typeModification) ? "seuil absolu" : "marge dynamique";
        boolean estAbsolu = "SEUIL_ABSOLU".equals(typeModification);

        String lignesValeurs = estAbsolu
                ? "<tr><td style='padding:6px 0;font-weight:600;width:160px;'>Valeur min</td>"
                +     "<td style='padding:6px 0;'>" + str(donneesEvenement, "valeurMin") + "</td></tr>"
                + "<tr style='background:#f8fafc;'><td style='padding:6px 0;font-weight:600;'>Valeur max</td>"
                +     "<td style='padding:6px 0;'>" + str(donneesEvenement, "valeurMax") + "</td></tr>"
                : "<tr><td style='padding:6px 0;font-weight:600;width:160px;'>Marge (±)</td>"
                +     "<td style='padding:6px 0;'>" + str(donneesEvenement, "marge") + "</td></tr>";

        String message = String.format(
                "Un administrateur a mis à jour la configuration du <strong>%s</strong> "
                + "pour le point de mesure <strong>%s</strong> (métrique : %s).<br><br>"
                + "<table style='border-collapse:collapse;width:100%%;font-size:14px;color:#475569;'>"
                + "<tr><td style='padding:6px 0;font-weight:600;width:160px;'>Point de mesure</td>"
                +     "<td style='padding:6px 0;'>%s</td></tr>"
                + "<tr style='background:#f8fafc;'><td style='padding:6px 0;font-weight:600;'>Métrique</td>"
                +     "<td style='padding:6px 0;'>%s</td></tr>"
                + "<tr><td style='padding:6px 0;font-weight:600;'>Type de modification</td>"
                +     "<td style='padding:6px 0;'>%s</td></tr>"
                + "<tr style='background:#f8fafc;'><td style='padding:6px 0;font-weight:600;'>Date</td>"
                +     "<td style='padding:6px 0;'>%s</td></tr>"
                + "</table>",
                typeLabel, nomPointMesure, metrique,
                nomPointMesure, metrique,
                "SEUIL_ABSOLU".equals(typeModification) ? "Seuil absolu" : "Marge dynamique",
                dateModification)
                + lignesValeurs;

        String urlSeuils = frontendUrl + "/seuils";
        return construireStructure(titre, message, "Voir les seuils configurés", urlSeuils, null);
    }

    // ── Structure commune ─────────────────────────────────────────────────────

    /**
     * Structure HTML commune à tous les templates.
     * Tous les 8 templates passent par cette méthode — aucune duplication de CSS/structure.
     *
     * @param titreSection   titre affiché dans le corps (h2) — toujours fourni par l'appelant
     * @param message        corps HTML (peut contenir des balises)
     * @param texteBouton    texte du bouton CTA (null = pas de bouton)
     * @param urlBouton      URL du bouton CTA (null = pas de bouton)
     * @param mentionValidite mention de validité en badge (null = absente)
     */
    public String construireStructure(
            String titreSection,
            String message,
            String texteBouton,
            String urlBouton,
            String mentionValidite
    ) {
        boolean hasButton = urlBouton != null && !urlBouton.isBlank()
                && texteBouton != null && !texteBouton.isBlank();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head>")
          .append("<meta charset='utf-8'>")
          .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
          .append("<title>").append(titreSection).append("</title>")
          .append("<style>")
          .append("body{font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;background-color:#f8fafc;color:#1e293b;margin:0;padding:0;-webkit-font-smoothing:antialiased;}")
          .append(".wrapper{max-width:600px;margin:40px auto;background:#fff;border-radius:16px;box-shadow:0 4px 6px -1px rgba(0,0,0,.05),0 2px 4px -2px rgba(0,0,0,.05);border:1px solid #e2e8f0;overflow:hidden;}")
          .append(".header{background:linear-gradient(135deg,#1e293b 0%,#0f172a 100%);padding:32px 24px;text-align:center;}")
          .append(".header-title{color:#f8fafc;font-size:20px;font-weight:700;letter-spacing:.5px;margin:0;text-transform:uppercase;}")
          .append(".content{padding:40px 32px;}")
          .append(".title{color:#0f172a;font-size:24px;font-weight:700;margin:0 0 20px 0;}")
          .append(".text{color:#475569;font-size:16px;line-height:1.6;margin:0 0 24px 0;}")
          .append(".button-container{text-align:center;margin:30px 0;}")
          .append(".btn{display:inline-block;background-color:#E88A2A;color:#fff!important;font-size:16px;font-weight:600;text-decoration:none;padding:14px 28px;border-radius:8px;box-shadow:0 4px 12px rgba(232,138,42,.25);}")
          .append(".fallback-container{background-color:#f1f5f9;border-radius:8px;padding:16px;margin:24px 0;word-break:break-all;border-left:4px solid #cbd5e1;}")
          .append(".fallback-title{font-size:12px;font-weight:600;color:#64748b;margin:0 0 8px 0;text-transform:uppercase;}")
          .append(".fallback-link{font-size:14px;color:#0284c7;text-decoration:none;}")
          .append(".badge-validity{display:inline-block;background-color:#fef3c7;color:#d97706;font-size:13px;font-weight:600;padding:6px 12px;border-radius:9999px;margin-bottom:24px;}")
          .append(".footer{background-color:#f8fafc;padding:24px;text-align:center;border-top:1px solid #e2e8f0;font-size:13px;color:#64748b;}")
          .append("</style></head><body>")
          .append("<div class='wrapper'>")
          .append("<div class='header'><h1 class='header-title'>Supervision Cabine de Peinture</h1></div>")
          .append("<div class='content'>")
          .append("<h2 class='title'>").append(titreSection).append("</h2>")
          .append("<p class='text'>").append(message.replace("\n", "<br>")).append("</p>");

        if (hasButton) {
            sb.append("<div class='button-container'>")
              .append("<a href='").append(urlBouton).append("' class='btn' target='_blank'>")
              .append(texteBouton).append("</a></div>")
              .append("<div class='fallback-container'>")
              .append("<p class='fallback-title'>Si le bouton ne fonctionne pas, copiez ce lien :</p>")
              .append("<a href='").append(urlBouton).append("' class='fallback-link' target='_blank'>")
              .append(urlBouton).append("</a></div>");
        }

        if (mentionValidite != null && !mentionValidite.isBlank()) {
            sb.append("<div class='badge-validity'>").append(mentionValidite).append("</div>");
        }

        sb.append("</div>")
          .append("<div class='footer'>© Supervision Cabine de Peinture | Tous droits réservés</div>")
          .append("</div></body></html>");

        return sb.toString();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> d, String cle) {
        if (d == null) return "";
        Object val = d.get(cle);
        return val != null ? val.toString() : "";
    }
}
