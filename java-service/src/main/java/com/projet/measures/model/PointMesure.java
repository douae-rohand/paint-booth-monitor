package com.projet.measures.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Module: measures
 * SQL Table: point_mesure
 *
 * Représente un point de mesure physique (capteur) dans le système.
 * - 1 cabine (température + humidité)
 * - 5 zones d'étuve (température uniquement)
 *
 * Écrit par Java (Admin active/désactive), lu par Python (pour associer nom_point_mesure lors de l'extraction/écriture).
 * Pas d'historisation par insertion : un point de mesure physique ne change pas de nom/type dans le temps.
 */
@Entity
@Table(name = "point_mesure")
public class PointMesure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "type_emplacement", nullable = false, length = 20)
    private String typeEmplacement;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // -- Constructeurs ---------------------------------------------------------

    public PointMesure() {}

    // -- Getters / Setters -----------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTypeEmplacement() {
        return typeEmplacement;
    }

    public void setTypeEmplacement(String typeEmplacement) {
        this.typeEmplacement = typeEmplacement;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
