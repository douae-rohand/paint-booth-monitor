package com.projet.reports.dto;

import com.projet.reports.model.RapportPDF;
import com.projet.reports.model.enums.StatutGeneration;
import com.projet.reports.model.enums.TypeRapport;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un rapport PDF.
 * Évite de sérialiser l'entité JPA brute (relations LAZY, mot de passe haché du superviseur, etc.)
 */
public class RapportPDFResponseDTO {

    private UUID idRapport;
    private PointMesureSummary pointMesure;
    private TypeRapport typeRapport;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;
    private String objetMinioStorageKey;
    private String nomFichier;
    private Long tailleFichier;
    private StatutGeneration statutGeneration;
    private LocalDateTime generatedAt;
    private LocalDateTime dateRapport;

    public static class PointMesureSummary {
        private Long id;
        private String nom;
        private String typeEmplacement;

        public PointMesureSummary(Long id, String nom, String typeEmplacement) {
            this.id = id;
            this.nom = nom;
            this.typeEmplacement = typeEmplacement;
        }

        public Long getId() { return id; }
        public String getNom() { return nom; }
        public String getTypeEmplacement() { return typeEmplacement; }
    }

    private RapportPDFResponseDTO() {}

    /**
     * Construit un DTO depuis l'entité JPA.
     * Doit être appelé dans un contexte transactionnel actif (relations LAZY encore accessibles).
     */
    public static RapportPDFResponseDTO from(RapportPDF rapport) {
        RapportPDFResponseDTO dto = new RapportPDFResponseDTO();
        dto.idRapport = rapport.getIdRapport();
        dto.typeRapport = rapport.getTypeRapport();
        dto.periodeDebut = rapport.getPeriodeDebut();
        dto.periodeFin = rapport.getPeriodeFin();
        dto.objetMinioStorageKey = rapport.getObjetMinioStorageKey();
        dto.nomFichier = rapport.getNomFichier();
        dto.tailleFichier = rapport.getTailleFichier();
        dto.statutGeneration = rapport.getStatutGeneration();
        dto.generatedAt = rapport.getGeneratedAt();
        dto.dateRapport = rapport.getDateRapport();

        if (rapport.getPointMesure() != null) {
            dto.pointMesure = new PointMesureSummary(
                    rapport.getPointMesure().getId(),
                    rapport.getPointMesure().getNom(),
                    rapport.getPointMesure().getTypeEmplacement()
            );
        }

        return dto;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getIdRapport() { return idRapport; }
    public PointMesureSummary getPointMesure() { return pointMesure; }
    public TypeRapport getTypeRapport() { return typeRapport; }
    public LocalDateTime getPeriodeDebut() { return periodeDebut; }
    public LocalDateTime getPeriodeFin() { return periodeFin; }
    public String getObjetMinioStorageKey() { return objetMinioStorageKey; }
    public String getNomFichier() { return nomFichier; }
    public Long getTailleFichier() { return tailleFichier; }
    public StatutGeneration getStatutGeneration() { return statutGeneration; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public LocalDateTime getDateRapport() { return dateRapport; }
}
