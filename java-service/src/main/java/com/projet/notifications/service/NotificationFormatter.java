package com.projet.notifications.service;

import com.projet.notifications.model.enums.TypeEvenement;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Formateur partagé pour le contenu textuel des notifications IN_APP.
 *
 * Responsabilité unique : convertir (TypeEvenement + donnees_evenement JSONB)
 * en texte lisible pour l'affichage côté frontend (toast + panel bell icon).
 *
 * Appelé par :
 *  - NotificationPushService  (push WebSocket initial → toast)
 *  - NotificationInAppService (lecture panel/historique → toDTO)
 *
 * Source unique de la logique de formatage IN_APP — aucune duplication possible.
 * Ne concerne pas l'email (SendGridEmailService a sa propre mise en forme HTML).
 */
@Component
public class NotificationFormatter {

    /**
     * Formate un contenu textuel lisible à partir des données brutes de l'événement.
     *
     * @param type    type de l'événement
     * @param donnees données brutes désérialisées depuis donnees_evenement JSONB
     * @return texte court lisible pour le toast et le panel bell
     */
    public String formaterContenuAffichage(TypeEvenement type, Map<String, Object> donnees) {
        if (type == null || donnees == null) return "";

        return switch (type) {
            case ALERTE_CREE -> String.format(
                    "Anomalie détectée sur %s — %s (%s). Sévérité : %s.",
                    str(donnees, "nomPointMesure"),
                    str(donnees, "metrique"),
                    str(donnees, "typeAlerte"),
                    str(donnees, "severite")
            );

            case ALERTE_RESOLU -> String.format(
                    "Anomalie résolue sur %s — %s. La valeur est revenue dans les bornes.",
                    str(donnees, "nomPointMesure"),
                    str(donnees, "metrique")
            );

            case COMPTE_ACTIVEE -> String.format(
                    "Le compte de %s %s vient d'être activé.",
                    str(donnees, "prenomSuperviseur"),
                    str(donnees, "nomSuperviseur")
            );

            case CONFIG_SEUILS_MODIFIE -> String.format(
                    "La configuration %s a été mise à jour pour %s (%s).",
                    "SEUIL_ABSOLU".equals(donnees.get("typeModification"))
                            ? "du seuil absolu"
                            : "de la marge dynamique",
                    str(donnees, "nomPointMesure"),
                    str(donnees, "metrique")
            );

            case RAPPORT_GENERE -> String.format(
                    "Un nouveau rapport (réf. %s) est disponible en téléchargement.",
                    str(donnees, "idRapport")
            );
        };
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> donnees, String cle) {
        Object val = donnees.get(cle);
        return val != null ? val.toString() : "";
    }
}
