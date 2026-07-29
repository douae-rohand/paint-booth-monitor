package com.projet.kpis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO commun pour les paramètres de période.
 * Utilisé sur plusieurs endpoints pour filtrer par période temporelle.
 */
@Data
public class PeriodeDTO {

    /**
     * Période prédéfinie : "24h", "7j", "30j", "3mois", "6mois", "1an", "personnalise"
     * Si "personnalise", les champs dateDebut et dateFin doivent être fournis.
     */
    private String periode;

    /**
     * Date de début de la période (utilisé uniquement si periode="personnalise")
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateDebut;

    /**
     * Date de fin de la période (utilisé uniquement si periode="personnalise")
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateFin;
}
