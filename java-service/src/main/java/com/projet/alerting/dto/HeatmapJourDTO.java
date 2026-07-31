package com.projet.alerting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour les données de heatmap par jour.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapJourDTO {

    /**
     * Jour du mois (1-31). Conservé pour compatibilité.
     */
    private Integer jour;

    /**
     * Date complète du jour (format YYYY-MM-DD).
     * Permet au frontend de positionner correctement chaque case
     * dans la grille sans ambiguïté de mois.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * Nombre de dépassements ce jour.
     */
    private Long nombreDepassements;
}
