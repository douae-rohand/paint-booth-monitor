package com.projet.reports.dto;

import com.projet.reports.model.enums.TypeRapport;

import java.time.LocalDateTime;

/**
 * DTO pour la demande de génération de rapport.
 * Module: reports
 */
public class RapportGenerationRequestDTO {

    private Long idPointMesure;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private TypeRapport typeRapport;

    // Getters et Setters
    public Long getIdPointMesure() {
        return idPointMesure;
    }

    public void setIdPointMesure(Long idPointMesure) {
        this.idPointMesure = idPointMesure;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public TypeRapport getTypeRapport() {
        return typeRapport;
    }

    public void setTypeRapport(TypeRapport typeRapport) {
        this.typeRapport = typeRapport;
    }
}
