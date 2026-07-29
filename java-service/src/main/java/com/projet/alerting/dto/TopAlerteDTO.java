package com.projet.alerting.dto;

import com.projet.alerting.model.enums.Metrique;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les top métriques en alerte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopAlerteDTO {

    /**
     * ID du point de mesure.
     */
    private Long idPointMesure;

    /**
     * Nom du point de mesure.
     */
    private String nomPointMesure;

    /**
     * Métrique concernée.
     */
    private Metrique metrique;

    /**
     * Nombre de dépassements sur la période.
     */
    private Long nombreDepassements;
}
