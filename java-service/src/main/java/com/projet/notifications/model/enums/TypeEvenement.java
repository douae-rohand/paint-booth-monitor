package com.projet.notifications.model.enums;

/**
 * Types d'événements déclenchant une notification.
 *
 * Utilisé dans :
 *  - notification.type_evenement  (table notification)
 *
 * Valeurs alignées avec la contrainte CHECK PostgreSQL (V36).
 * Ne pas ajouter de valeur ici sans créer une migration Flyway correspondante.
 */
public enum TypeEvenement {

    /** Nouvelle alerte créée (seuil absolu, dynamique ou IA). */
    ALERTE_CREE,

    /** Alerte passée au statut RESOLUE (valeur revenue dans les bornes). */
    ALERTE_RESOLU,

    /** Rapport PDF généré et disponible en téléchargement. */
    RAPPORT_GENERE,

    /** Compte superviseur activé (lien d'activation cliqué). */
    COMPTE_ACTIVEE,

    /** Configuration des seuils absolu ou dynamique modifiée par un Admin. */
    CONFIG_SEUILS_MODIFIE
}
