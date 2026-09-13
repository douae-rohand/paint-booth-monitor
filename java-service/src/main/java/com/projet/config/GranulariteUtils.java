package com.projet.config;

import com.projet.config.model.enums.Granularite;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utilitaire transverse pour la détermination automatique de la granularité.
 */
public final class GranulariteUtils {

    private GranulariteUtils() {
        // Constructeur privé pour classe utilitaire
    }

    /**
     * Détermine la granularité automatique selon l'écart entre la date de début et la date de fin.
     *
     * Règles :
     * - Écart <= 1 jour   -> TRENTE_MIN
     * - Écart <= 7 jours  -> HORAIRE
     * - Écart <= 31 jours -> JOURNALIERE
     * - Écart > 31 jours  -> MENSUELLE
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Granularité appropriée
     */
    public static Granularite determinerDepuisEcart(LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (dateDebut == null || dateFin == null) {
            return Granularite.JOURNALIERE;
        }
        long jours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        if (jours <= 1) {
            return Granularite.TRENTE_MIN;
        } else if (jours <= 7) {
            return Granularite.HORAIRE;
        } else if (jours <= 31) {
            return Granularite.JOURNALIERE;
        } else {
            return Granularite.MENSUELLE;
        }
    }

    /**
     * Détermine la granularité à appliquer selon la période prédéfinie ("24h", "7j", "30j", "6mois", "1an", "personnalise")
     * ou l'écart de dates.
     *
     * @param periode Période prédéfinie
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @param granulariteDemandee Granularité demandée par l'utilisateur (optionnel, pour "7j")
     * @return Granularité appropriée
     */
    public static Granularite determinerDepuisPeriode(
            String periode,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Granularite granulariteDemandee) {

        if (periode == null) {
            return determinerDepuisEcart(dateDebut, dateFin);
        }

        switch (periode) {
            case "24h":
                return Granularite.TRENTE_MIN;

            case "7j":
                if (granulariteDemandee != null) {
                    if (granulariteDemandee == Granularite.HORAIRE || granulariteDemandee == Granularite.JOURNALIERE) {
                        return granulariteDemandee;
                    }
                }
                return Granularite.HORAIRE;

            case "30j":
                return Granularite.JOURNALIERE;

            case "6mois":
            case "1an":
                return Granularite.MENSUELLE;

            case "personnalise":
                return determinerDepuisEcart(dateDebut, dateFin);

            default:
                return Granularite.JOURNALIERE;
        }
    }
}
