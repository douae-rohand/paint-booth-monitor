package com.projet.alerting.model;

import com.projet.alerting.model.enums.Metrique;
import com.projet.auth.model.Admin;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: alerting
 * SQL Table: seuil_absolu
 * 
 * 100% Java-managed entity (configured by Admin). Read by Python service 
 * in read-only mode during ingestion. Java has read/write privileges.
 */
@Entity
@Table(name = "seuil_absolu")
public class SeuilAbsolu {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_seuil_absolu")
    private UUID idSeuilAbsolu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Metrique metrique;

    @Column(name = "valeur_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal valeurMin;

    @Column(name = "valeur_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal valeurMax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public SeuilAbsolu() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdSeuilAbsolu() { return idSeuilAbsolu; }
    public void setIdSeuilAbsolu(UUID idSeuilAbsolu) { this.idSeuilAbsolu = idSeuilAbsolu; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public Metrique getMetrique() { return metrique; }
    public void setMetrique(Metrique metrique) { this.metrique = metrique; }

    public BigDecimal getValeurMin() { return valeurMin; }
    public void setValeurMin(BigDecimal valeurMin) { this.valeurMin = valeurMin; }

    public BigDecimal getValeurMax() { return valeurMax; }
    public void setValeurMax(BigDecimal valeurMax) { this.valeurMax = valeurMax; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
