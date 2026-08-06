package com.projet.alerting.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour la consultation des alertes.
 * Module: alerting
 */
public record AlerteDTO(
    UUID idAlerte,
    LocalDateTime dateCreation,
    LocalDateTime dateResolution,
    String pointMesureNom,
    String metrique,
    String typeAlerte,
    String severite,
    String statut,
    Long dureeMinutes
) {}
