package com.projet.auth.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Module: auth
 * SQL Table: admin
 * 
 * Admin role extension. Inherits identity from Superviseur via a OneToOne relation
 * sharing the same primary key (id_admin). Written and read by Java service.
 */
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @Column(name = "id_admin")
    private UUID idAdmin;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_admin")
    private Superviseur superviseur;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Admin() {}

    public Admin(Superviseur superviseur) {
        this.superviseur = superviseur;
        if (superviseur != null) {
            this.idAdmin = superviseur.getIdSuperviseur();
        }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdAdmin() { return idAdmin; }
    public void setIdAdmin(UUID idAdmin) { this.idAdmin = idAdmin; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) {
        this.superviseur = superviseur;
        if (superviseur != null) {
            this.idAdmin = superviseur.getIdSuperviseur();
        }
    }
}
