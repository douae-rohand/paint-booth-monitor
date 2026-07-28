package com.projet.auth.dto;

import java.util.UUID;

public class SuperviseurListItemDTO {
    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private boolean actif;
    private boolean compteActive;

    public SuperviseurListItemDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public boolean isCompteActive() { return compteActive; }
    public void setCompteActive(boolean compteActive) { this.compteActive = compteActive; }
}
