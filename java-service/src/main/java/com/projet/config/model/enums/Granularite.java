package com.projet.config.model.enums;

/**
 * Enum pour la granularité d'agrégation des mesures (transverse à l'application).
 */
public enum Granularite {
    /**
     * Agrégation par tranches de 30 minutes.
     */
    TRENTE_MIN,

    /**
     * Agrégation horaire.
     */
    HORAIRE,

    /**
     * Agrégation journalière.
     */
    JOURNALIERE,

    /**
     * Agrégation mensuelle.
     */
    MENSUELLE
}
