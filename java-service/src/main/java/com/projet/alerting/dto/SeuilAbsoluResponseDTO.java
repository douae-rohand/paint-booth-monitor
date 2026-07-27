package com.projet.alerting.dto;

import com.projet.alerting.model.enums.Metrique;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SeuilAbsoluResponseDTO {
    private UUID id;
    private Long idPointMesure;
    private String nomPointMesure;
    private Metrique metrique;
    private BigDecimal valeurMin;
    private BigDecimal valeurMax;
    private boolean actif;
    private LocalDateTime createdAt;
    private LocalDateTime dateActivation;
    private LocalDateTime dateDesactivation;

    public SeuilAbsoluResponseDTO() {}

    public SeuilAbsoluResponseDTO(UUID id, Long idPointMesure, String nomPointMesure, Metrique metrique,
                                  BigDecimal valeurMin, BigDecimal valeurMax, boolean actif,
                                  LocalDateTime createdAt, LocalDateTime dateActivation, LocalDateTime dateDesactivation) {
        this.id = id;
        this.idPointMesure = idPointMesure;
        this.nomPointMesure = nomPointMesure;
        this.metrique = metrique;
        this.valeurMin = valeurMin;
        this.valeurMax = valeurMax;
        this.actif = actif;
        this.createdAt = createdAt;
        this.dateActivation = dateActivation;
        this.dateDesactivation = dateDesactivation;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getIdPointMesure() {
        return idPointMesure;
    }

    public void setIdPointMesure(Long idPointMesure) {
        this.idPointMesure = idPointMesure;
    }

    public String getNomPointMesure() {
        return nomPointMesure;
    }

    public void setNomPointMesure(String nomPointMesure) {
        this.nomPointMesure = nomPointMesure;
    }

    public Metrique getMetrique() {
        return metrique;
    }

    public void setMetrique(Metrique metrique) {
        this.metrique = metrique;
    }

    public BigDecimal getValeurMin() {
        return valeurMin;
    }

    public void setValeurMin(BigDecimal valeurMin) {
        this.valeurMin = valeurMin;
    }

    public BigDecimal getValeurMax() {
        return valeurMax;
    }

    public void setValeurMax(BigDecimal valeurMax) {
        this.valeurMax = valeurMax;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDateActivation() {
        return dateActivation;
    }

    public void setDateActivation(LocalDateTime dateActivation) {
        this.dateActivation = dateActivation;
    }

    public LocalDateTime getDateDesactivation() {
        return dateDesactivation;
    }

    public void setDateDesactivation(LocalDateTime dateDesactivation) {
        this.dateDesactivation = dateDesactivation;
    }
}
