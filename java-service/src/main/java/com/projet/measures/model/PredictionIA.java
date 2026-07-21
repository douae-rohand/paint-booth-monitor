package com.projet.measures.model;

import com.projet.measures.model.enums.ModeleIA;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: measures
 * SQL Table: prediction_ia
 * 
 * LECTURE SEULE (READ-ONLY) from Java-service.
 * This table is written to EXCLUSIVELY by the python-service AI module.
 * 
 * Note: id_mesure is mapped as a simple UUID to avoid cross-module coupling.
 * 
 * IMPORTANT JAVADOC NOTE: NE JAMAIS APPELER save() OU MODIFIER CETTE ENTITE 
 * DEPUIS JAVA under any circumstances. Writing is reserved for the Python service.
 */
@Entity
@Table(name = "prediction_ia")
public class PredictionIA {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_prediction", updatable = false, insertable = false)
    private UUID idPrediction;

    // Simple UUID to avoid FK JPA active references to Mesure
    @Column(name = "id_mesure", nullable = false, updatable = false, insertable = false)
    private UUID idMesure;

    @Enumerated(EnumType.STRING)
    @Column(name = "modele_utilise", nullable = false, length = 20, updatable = false, insertable = false)
    private ModeleIA modeleUtilise;

    @Column(name = "valeur_predite", precision = 6, scale = 2, updatable = false, insertable = false)
    private BigDecimal valeurPredite;

    @Column(name = "valeur_reelle", precision = 6, scale = 2, updatable = false, insertable = false)
    private BigDecimal valeurReelle;

    @Column(name = "est_anomalie", updatable = false, insertable = false)
    private Boolean estAnomalie;

    @Column(name = "date_prediction", nullable = false, updatable = false, insertable = false)
    private LocalDateTime datePrediction;

    @Column(name = "updated_at", updatable = false, insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", updatable = false, insertable = false)
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PredictionIA() {}

    // ── Getters Only (Enforcing Read-Only Design) ─────────────────────────────

    public UUID getIdPrediction() { return idPrediction; }

    public UUID getIdMesure() { return idMesure; }

    public ModeleIA getModeleUtilise() { return modeleUtilise; }

    public BigDecimal getValeurPredite() { return valeurPredite; }

    public BigDecimal getValeurReelle() { return valeurReelle; }

    public Boolean getEstAnomalie() { return estAnomalie; }

    public LocalDateTime getDatePrediction() { return datePrediction; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
}
