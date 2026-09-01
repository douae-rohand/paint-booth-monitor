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
                    "Anomalie détectée sur %s - %s (%s).",
                    str(donnees, "nomPointMesure"),
                    str(donnees, "metrique"),
                    str(donnees, "typeAlerte")
            );

            case ALERTE_RESOLU -> String.format(
                    "Anomalie résolue sur %s - %s.",
                    str(donnees, "nomPointMesure"),
                    str(donnees, "metrique")
            );

            case COMPTE_ACTIVEE -> String.format(
                    "Le compte de %s %s vient d'être activé.",
                    str(donnees, "prenomSuperviseur"),
                    str(donnees, "nomSuperviseur")
            );

            case CONFIG_SEUILS_MODIFIE -> {
                String typeLabel = "SEUIL_ABSOLU".equals(donnees.get("typeModification"))
                        ? "du seuil absolu" : "de la marge dynamique";
                boolean estAbsolu = "SEUIL_ABSOLU".equals(donnees.get("typeModification"));
                if (estAbsolu) {
                    yield String.format(
                            "La configuration %s pour: %s (%s), Min : %s, Max : %s.",
                            typeLabel,
                            str(donnees, "nomPointMesure"),
                            str(donnees, "metrique"),
                            str(donnees, "valeurMin"),
                            str(donnees, "valeurMax")
                    );
                } else {
                    yield String.format(
                            "La configuration %s pour: %s (%s), Marge : ±%s.",
                            typeLabel,
                            str(donnees, "nomPointMesure"),
                            str(donnees, "metrique"),
                            str(donnees, "marge")
                    );
                }
            }
        };
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> donnees, String cle) {
        Object val = donnees.get(cle);
        return val != null ? val.toString() : "";
    }
}
