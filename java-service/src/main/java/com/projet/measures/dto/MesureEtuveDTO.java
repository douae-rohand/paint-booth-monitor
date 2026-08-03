package com.projet.measures.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour l'historique des mesures de l'étuve.
 * Contient les valeurs de température par zone.
 */
public record MesureEtuveDTO(
    UUID idMesure,
    LocalDateTime dateMesure,
    String zone,
    BigDecimal temperature,
    boolean depassement
) {
}
