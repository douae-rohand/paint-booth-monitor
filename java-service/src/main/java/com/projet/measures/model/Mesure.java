package com.projet.measures.model;

import com.projet.measures.model.enums.Metrique;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: measures
 * SQL Table: mesure
 * 
 * LECTURE SEULE (READ-ONLY) from Java-service.
 * This table is written to EXCLUSIVELY by the python-service PLC module.
 * 
 * IMPORTANT JAVADOC NOTE: NE JAMAIS APPELER save() OU ENREGISTRER CETTE ENTITE 
 * DEPUIS JAVA under any circumstances. Writing is reserved for the Python service.
 */
@Entity
@Table(name = "mesure")
public class Mesure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_mesure", updatable = false, insertable = false)
    private UUID idMesure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false, insertable = false)
    private Metrique metrique;

    @Column(nullable = false, precision = 6, scale = 2, updatable = false, insertable = false)
    private BigDecimal valeur;

    @Column(name = "identifiant_caisse", length = 100, updatable = false, insertable = false)
    private String identifiantCaisse;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Mesure() {}

    // ── Getters Only (Enforcing Read-Only Design) ─────────────────────────────

    public UUID getIdMesure() { return idMesure; }

    public Metrique getMetrique() { return metrique; }

    public BigDecimal getValeur() { return valeur; }

    public String getIdentifiantCaisse() { return identifiantCaisse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
