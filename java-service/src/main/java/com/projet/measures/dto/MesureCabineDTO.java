package com.projet.measures.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour l'historique des mesures de la cabine de peinture.
 * Contient les valeurs de température et d'humidité pivotées par cycle de mesure.
 */
public record MesureCabineDTO(
    LocalDateTime timestampCycle,
    String caisseId,
    BigDecimal temperature,
    BigDecimal humidite,
    boolean depassementTemperature,
    boolean depassementHumidite
) {
}
