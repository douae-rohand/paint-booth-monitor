package com.projet.measures.model.enums;

/**
 * Enum pour la granularité d'agrégation des mesures historiques.
 * Module: measures
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
