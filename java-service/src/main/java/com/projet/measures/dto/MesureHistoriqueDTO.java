package com.projet.measures.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour un point de données d'historique de mesure agrégé.
 * L'horodatage est tronqué/arrondi selon la granularité d'agrégation.
 * La valeur est la moyenne agrégée des mesures dans le bucket temporel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MesureHistoriqueDTO {

    /**
     * Horodatage tronqué/arrondi selon la granularité d'agrégation.
     * Représente le début du bucket temporel.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horodatage;

    /**
     * Valeur moyenne agrégée des mesures dans le bucket temporel.
     */
    private BigDecimal valeur;
}
