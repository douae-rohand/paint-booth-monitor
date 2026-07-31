package com.projet.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projet.alerting.model.enums.Metrique;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message WebSocket pour une nouvelle mesure.
 * Publié sur /topic/mesures/{idPointMesure}/{metrique} lors de l'insertion d'une mesure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MesureMessage {

    private Long idPointMesure;
    private String nomPointMesure;
    private Metrique metrique;
    private BigDecimal valeur;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
