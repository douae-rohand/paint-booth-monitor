package com.projet.measures.dto;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.model.PointMesure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de réponse pour un point de mesure.
 * Expose les informations d'un point de mesure physique (cabine ou zone d'étuve).
 */
public class PointMesureResponse {

    private Long id;
    private String nom;
    private String typeEmplacement;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<String> metriquesApplicables;

    // -- Constructeurs ---------------------------------------------------------

    public PointMesureResponse() {}

    /**
     * Construit un DTO à partir de l'entité JPA.
     */
    public static PointMesureResponse from(PointMesure entity) {
        PointMesureResponse dto = new PointMesureResponse();
        dto.id = entity.getId();
        dto.nom = entity.getNom();
        dto.typeEmplacement = entity.getTypeEmplacement();
        dto.actif = entity.isActif();
        dto.dateCreation = entity.getDateCreation();
        dto.updatedAt = entity.getUpdatedAt();
        dto.deletedAt = entity.getDeletedAt();
        dto.metriquesApplicables = calculerMetriquesApplicables(entity.getTypeEmplacement());
        return dto;
    }

    /**
     * Calcule les métriques applicables selon le type d'emplacement.
     * CABINE → [TEMPERATURE, HUMIDITE]
     * ETUVE → [TEMPERATURE]
     *
     * @param typeEmplacement Type d'emplacement
     * @return Liste des noms de métriques applicables
     */
    private static List<String> calculerMetriquesApplicables(String typeEmplacement) {
        List<String> metriques = new ArrayList<>();

        if ("CABINE".equalsIgnoreCase(typeEmplacement)) {
            metriques.add(Metrique.TEMPERATURE.name());
            metriques.add(Metrique.HUMIDITE.name());
        } else if ("ETUVE".equalsIgnoreCase(typeEmplacement)) {
            metriques.add(Metrique.TEMPERATURE.name());
        }

        return metriques;
    }

    // -- Getters ---------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTypeEmplacement() {
        return typeEmplacement;
    }

    public boolean isActif() {
        return actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public List<String> getMetriquesApplicables() {
        return metriquesApplicables;
    }
}
