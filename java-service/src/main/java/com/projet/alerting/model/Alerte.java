package com.projet.alerting.model;

import com.projet.alerting.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: alerting
 * SQL Table: alerte
 * 
 * HYBRID / SHARED ENTITY.
 * - Python-service creates alerts (inserts id_alerte, id_mesure, metrique, 
 *   type_alerte, severite, created_at) at ingestion when thresholds are exceeded.
 * - Java-service is ONLY allowed to update "statut", "updatedAt", and "deletedAt" 
 *   (e.g., resolving an alert ACTIVE -> RESOLUE).
 * 
 * IMPORTANT JAVADOC NOTE: Java should never attempt to write or edit fields 
 * other than: "statut", "updatedAt", and "deletedAt". All other fields 
 * are marked in JPA with "updatable = false".
 */
@Entity
@Table(name = "alerte")
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_alerte")
    private UUID idAlerte;

    // Simple UUID to avoid cross-module coupling and direct writes to mesures
    @Column(name = "id_mesure", nullable = false, updatable = false)
    private UUID idMesure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Metrique metrique;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_alerte", nullable = false, length = 20, updatable = false)
    private TypeAlerte typeAlerte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private Severite severite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutAlerte statut = StatutAlerte.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Alerte() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdAlerte() { return idAlerte; }
    public void setIdAlerte(UUID idAlerte) { this.idAlerte = idAlerte; }

    public UUID getIdMesure() { return idMesure; }
    public void setIdMesure(UUID idMesure) { this.idMesure = idMesure; }

    public Metrique getMetrique() { return metrique; }
    public void setMetrique(Metrique metrique) { this.metrique = metrique; }

    public TypeAlerte getTypeAlerte() { return typeAlerte; }
    public void setTypeAlerte(TypeAlerte typeAlerte) { this.typeAlerte = typeAlerte; }

    public Severite getSeverite() { return severite; }
    public void setSeverite(Severite severite) { this.severite = severite; }

    public StatutAlerte getStatut() { return statut; }
    public void setStatut(StatutAlerte statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
