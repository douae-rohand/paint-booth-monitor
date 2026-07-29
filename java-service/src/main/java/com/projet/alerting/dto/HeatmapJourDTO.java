package com.projet.alerting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les données de heatmap par jour.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapJourDTO {

    /**
     * Jour du mois (1-31).
     */
    private Integer jour;

    /**
     * Nombre de dépassements ce jour.
     */
    private Long nombreDepassements;
}
