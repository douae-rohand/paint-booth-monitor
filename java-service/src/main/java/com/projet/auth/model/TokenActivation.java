package com.projet.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: auth
 * SQL Table: token_activation
 * 
 * Token used to verify account activation requests. Written and read by Java service.
 */
@Entity
@Table(name = "token_activation")
public class TokenActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_token_activation")
    private UUID idTokenActivation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private boolean utilise = false;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────────────────────

    public TokenActivation() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdTokenActivation() { return idTokenActivation; }
    public void setIdTokenActivation(UUID idTokenActivation) { this.idTokenActivation = idTokenActivation; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public boolean isUtilise() { return utilise; }
    public void setUtilise(boolean utilise) { this.utilise = utilise; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
