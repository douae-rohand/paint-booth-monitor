package com.projet.reports.model;

import com.projet.auth.model.Superviseur;
import com.projet.measures.model.PointMesure;
import com.projet.reports.model.enums.StatutGeneration;
import com.projet.reports.model.enums.TypeRapport;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: reports
 * SQL Table: rapport_pdf
 *
 * Written and read by Java service. Tracks PDF report generation requests
 * and their current state.
 *
 * Règle de remplissage des colonnes optionnelles :
 *   objet_minio_storage_key, nom_fichier, taille_fichier, generated_at
 *   → nullable, remplis uniquement après génération réussie (statut = TERMINE).
 *
 * objet_minio_storage_key stocke une clé d'objet MinIO, pas un chemin disque local.
 * Format : rapports/{annee}/{mois}/{id_rapport}.pdf
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

    /**
     * Point de mesure concerné par le rapport.
     * Même pattern que Mesure/SeuilAbsolu/SeuilDynamique : @ManyToOne LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_point_mesure", nullable = false)
    private PointMesure pointMesure;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_rapport", nullable = false, length = 20)
    private TypeRapport typeRapport;

    /**
     * Début de la période couverte par le rapport.
     * Type LocalDateTime (vs LocalDate en V13) pour supporter les périodes
     * personnalisées courtes avec précision heure/minute.
     */
    @Column(name = "periode_debut", nullable = false)
    private LocalDateTime periodeDebut;

    /**
     * Fin de la période couverte par le rapport.
     * Même logique que periodeDebut.
     */
    @Column(name = "periode_fin", nullable = false)
    private LocalDateTime periodeFin;

    /**
     * Clé d'objet MinIO — pas un chemin disque local.
     * Format : rapports/{annee}/{mois}/{id_rapport}.pdf
     * Rempli uniquement après génération réussie.
     */
    @Column(name = "objet_minio_storage_key", length = 500)
    private String objetMinioStorageKey;

    /**
     * Nom du fichier PDF présenté à l'utilisateur (ex: rapport-cabine-2026-08.pdf).
     * Rempli uniquement après génération réussie.
     */
    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    /**
     * Taille du fichier PDF en octets.
     * Rempli uniquement après génération réussie.
     */
    @Column(name = "taille_fichier")
    private Long tailleFichier;

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

    public PointMesure getPointMesure() { return pointMesure; }
    public void setPointMesure(PointMesure pointMesure) { this.pointMesure = pointMesure; }

    public TypeRapport getTypeRapport() { return typeRapport; }
    public void setTypeRapport(TypeRapport typeRapport) { this.typeRapport = typeRapport; }

    public LocalDateTime getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDateTime periodeDebut) { this.periodeDebut = periodeDebut; }

    public LocalDateTime getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(LocalDateTime periodeFin) { this.periodeFin = periodeFin; }

    public String getObjetMinioStorageKey() { return objetMinioStorageKey; }
    public void setObjetMinioStorageKey(String objetMinioStorageKey) { this.objetMinioStorageKey = objetMinioStorageKey; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public Long getTailleFichier() { return tailleFichier; }
    public void setTailleFichier(Long tailleFichier) { this.tailleFichier = tailleFichier; }

    public StatutGeneration getStatutGeneration() { return statutGeneration; }
    public void setStatutGeneration(StatutGeneration statutGeneration) { this.statutGeneration = statutGeneration; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getDateRapport() { return dateRapport; }
    public void setDateRapport(LocalDateTime dateRapport) { this.dateRapport = dateRapport; }
}
