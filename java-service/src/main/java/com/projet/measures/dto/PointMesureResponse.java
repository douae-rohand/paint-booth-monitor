package com.projet.measures.dto;

import com.projet.measures.model.PointMesure;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un point de mesure.
 * Expose les informations d'un point de mesure physique (cabine ou zone d'étuve).
 */
public class PointMesureResponse {

    private Long id;
    private String nom;
    private String typeEmplacement;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // -- Constructeurs ---------------------------------------------------------

    public PointMesureResponse() {}

    /**
     * Construit un DTO à partir de l'entité JPA.
     */
    public static PointMesureResponse from(PointMesure entity) {
        PointMesureResponse dto = new PointMesureResponse();
        dto.id = entity.getId();
        dto.nom = entity.getNom();
        dto.typeEmplacement = entity.getTypeEmplacement();
        dto.actif = entity.isActif();
        dto.dateCreation = entity.getDateCreation();
        dto.updatedAt = entity.getUpdatedAt();
        dto.deletedAt = entity.getDeletedAt();
        return dto;
    }

    // -- Getters ---------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTypeEmplacement() {
        return typeEmplacement;
    }

    public boolean isActif() {
        return actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
