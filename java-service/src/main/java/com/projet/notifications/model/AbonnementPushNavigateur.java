package com.projet.notifications.model;

import com.projet.auth.model.Superviseur;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Module: notifications
 * SQL Table: abonnement_push_navigateur
 *
 * Représente un abonnement Web Push (VAPID) pour un superviseur.
 * Stocke l'endpoint et les clés de chiffrement fournies par le navigateur.
 * Un superviseur peut avoir plusieurs abonnements (plusieurs navigateurs/appareils).
 */
@Entity
@Table(name = "abonnement_push_navigateur")
public class AbonnementPushNavigateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Column(name = "endpoint", nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "cle_p256dh", nullable = false, columnDefinition = "TEXT")
    private String cleP256dh;

    @Column(name = "cle_auth", nullable = false, columnDefinition = "TEXT")
    private String cleAuth;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // Constructeurs
    public AbonnementPushNavigateur() {}

    public AbonnementPushNavigateur(Superviseur superviseur, String endpoint, String cleP256dh, String cleAuth, String userAgent) {
        this.superviseur = superviseur;
        this.endpoint = endpoint;
        this.cleP256dh = cleP256dh;
        this.cleAuth = cleAuth;
        this.userAgent = userAgent;
    }

    // Getters et Setters
    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public Superviseur getSuperviseur() {
        return superviseur;
    }

    public void setSuperviseur(Superviseur superviseur) {
        this.superviseur = superviseur;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getCleP256dh() {
        return cleP256dh;
    }

    public void setCleP256dh(String cleP256dh) {
        this.cleP256dh = cleP256dh;
    }

    public String getCleAuth() {
        return cleAuth;
    }

    public void setCleAuth(String cleAuth) {
        this.cleAuth = cleAuth;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
