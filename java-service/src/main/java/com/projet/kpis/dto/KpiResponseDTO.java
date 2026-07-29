package com.projet.kpis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les KPIs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponseDTO {

    /**
     * Nombre d'alertes actives (statut = ACTIVE).
     * C'est un instantané, pas dépendant de la période.
     */
    private Long alertesActives;

    /**
     * Nombre de points de mesure en anomalie (ayant au moins une alerte active).
     */
    private Long nbPointsEnAnomalie;

    /**
     * Nombre total de points de mesure actifs non supprimés.
     */
    private Long nbPointsTotal;

    /**
     * Taux de conformité en pourcentage.
     * Calculé uniquement si un scope point+métrique est fourni.
     * Null si aucune donnée sur la période ou si aucun seuil configuré.
     */
    private Double tauxConformite;

    /**
     * Temps moyen entre incidents en heures.
     * Calculé uniquement si un scope point+métrique est fourni.
     * Null si moins de 2 alertes SEUIL_ABSOLU sur la période.
     */
    private Double tempsMoyenEntreIncidentsHeures;

    /**
     * Temps moyen de retour à la normale en heures.
     * Calculé uniquement si un scope point+métrique est fourni.
     * Null si aucune alerte résolue sur la période.
     */
    private Double tempsMoyenRetourNormalHeures;
}
