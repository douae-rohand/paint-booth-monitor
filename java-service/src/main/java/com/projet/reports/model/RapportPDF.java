package com.projet.reports.model;

import com.projet.auth.model.Superviseur;
import com.projet.reports.model.enums.StatutGeneration;
import com.projet.reports.model.enums.TypeRapport;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: reports
 * SQL Table: rapport_pdf
 *
 * Written and read by Java service. Tracks PDF report generation requests 
 * and their current state.
 */
@Entity
@Table(name = "rapport_pdf")
public class RapportPDF {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_rapport")
    private UUID idRapport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_rapport", nullable = false, length = 20)
    private TypeRapport typeRapport;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "chemin_fichier", length = 500)
    private String cheminFichier;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_generation", nullable = false, length = 20)
    private StatutGeneration statutGeneration = StatutGeneration.EN_COURS;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "date_rapport", nullable = false, updatable = false)
    private LocalDateTime dateRapport = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────────────────────

    public RapportPDF() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdRapport() { return idRapport; }
    public void setIdRapport(UUID idRapport) { this.idRapport = idRapport; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }

    public TypeRapport getTypeRapport() { return typeRapport; }
    public void setTypeRapport(TypeRapport typeRapport) { this.typeRapport = typeRapport; }

    public LocalDate getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDate periodeDebut) { this.periodeDebut = periodeDebut; }

    public LocalDate getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(LocalDate periodeFin) { this.periodeFin = periodeFin; }

    public String getCheminFichier() { return cheminFichier; }
    public void setCheminFichier(String cheminFichier) { this.cheminFichier = cheminFichier; }

    public StatutGeneration getStatutGeneration() { return statutGeneration; }
    public void setStatutGeneration(StatutGeneration statutGeneration) { this.statutGeneration = statutGeneration; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getDateRapport() { return dateRapport; }
    public void setDateRapport(LocalDateTime dateRapport) { this.dateRapport = dateRapport; }
}
