package com.projet.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: auth
 * SQL Table: token_reinitialisation
 * 
 * Token used to verify password reset requests. Written and read by Java service.
 */
@Entity
@Table(name = "token_reinitialisation")
public class TokenReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_token_reset")
    private UUID idTokenReset;

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

    public TokenReinitialisation() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdTokenReset() { return idTokenReset; }
    public void setIdTokenReset(UUID idTokenReset) { this.idTokenReset = idTokenReset; }

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
