package com.projet.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SuperviseurResponseDTO {
    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private boolean actif;
    private boolean compteActive;
    private LocalDateTime createdAt;
    private LocalDateTime dateExpirationActivation;

    public SuperviseurResponseDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public boolean isCompteActive() { return compteActive; }
    public void setCompteActive(boolean compteActive) { this.compteActive = compteActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDateExpirationActivation() { return dateExpirationActivation; }
    public void setDateExpirationActivation(LocalDateTime dateExpirationActivation) { this.dateExpirationActivation = dateExpirationActivation; }
}
