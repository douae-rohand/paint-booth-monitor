package com.projet.measures.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour un point de données d'historique de mesure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MesureHistoriqueDTO {

    /**
     * Timestamp de la mesure.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Valeur de la mesure.
     */
    private BigDecimal valeur;
}
