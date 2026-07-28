package com.projet.auth.dto;

public class ActivationCompteDTO {
    private String token;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;

    public ActivationCompteDTO() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNouveauMotDePasse() { return nouveauMotDePasse; }
    public void setNouveauMotDePasse(String nouveauMotDePasse) { this.nouveauMotDePasse = nouveauMotDePasse; }

    public String getConfirmationMotDePasse() { return confirmationMotDePasse; }
    public void setConfirmationMotDePasse(String confirmationMotDePasse) { this.confirmationMotDePasse = confirmationMotDePasse; }
}
