package com.projet.alerting.model;

import com.projet.alerting.model.enums.Metrique;
import com.projet.auth.model.Admin;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: alerting
 * SQL Table: seuil_dynamique
 * 
 * HYBRID / SHARED ENTITY.
 * - Java Admin configures and modifies only the fields: "metrique", 
 *   "margeConfiguree", and soft-delete "deletedAt".
 * - Python-service calculates continuously the fields: "valeurMinCalculee", 
 *   "valeurMaxCalculee", and "dateCalcul".
 * 
 * IMPORTANT JAVADOC NOTE: Java code must refresh/reload this entity before 
 * saving, and must avoid setting/updating the Python-calculated fields to 
 * avoid overwriting Python calculations.
 */
@Entity
@Table(name = "seuil_dynamique")
public class SeuilDynamique {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_seuil_dynamique")
    private UUID idSeuilDynamique;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Metrique metrique;

    // Written only by Python service
    @Column(name = "valeur_min_calculee", precision = 6, scale = 2)
    private BigDecimal valeurMinCalculee;

    // Written only by Python service
    @Column(name = "valeur_max_calculee", precision = 6, scale = 2)
    private BigDecimal valeurMaxCalculee;

    // Configured by Java Admin
    @Column(name = "marge_configuree", nullable = false, precision = 6, scale = 2)
    private BigDecimal margeConfiguree;

    // Recalculated by Python service
    @Column(name = "date_calcul")
    private LocalDateTime dateCalcul;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public SeuilDynamique() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdSeuilDynamique() { return idSeuilDynamique; }
    public void setIdSeuilDynamique(UUID idSeuilDynamique) { this.idSeuilDynamique = idSeuilDynamique; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public Metrique getMetrique() { return metrique; }
    public void setMetrique(Metrique metrique) { this.metrique = metrique; }

    public BigDecimal getValeurMinCalculee() { return valeurMinCalculee; }
    public void setValeurMinCalculee(BigDecimal valeurMinCalculee) { this.valeurMinCalculee = valeurMinCalculee; }

    public BigDecimal getValeurMaxCalculee() { return valeurMaxCalculee; }
    public void setValeurMaxCalculee(BigDecimal valeurMaxCalculee) { this.valeurMaxCalculee = valeurMaxCalculee; }

    public BigDecimal getMargeConfiguree() { return margeConfiguree; }
    public void setMargeConfiguree(BigDecimal margeConfiguree) { this.margeConfiguree = margeConfiguree; }

    public LocalDateTime getDateCalcul() { return dateCalcul; }
    public void setDateCalcul(LocalDateTime dateCalcul) { this.dateCalcul = dateCalcul; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
