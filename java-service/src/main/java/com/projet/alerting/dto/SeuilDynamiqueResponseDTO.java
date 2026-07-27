package com.projet.alerting.dto;

import com.projet.alerting.model.enums.Metrique;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SeuilDynamiqueResponseDTO {
    private UUID id;
    private Long idPointMesure;
    private String nomPointMesure;
    private Metrique metrique;
    private BigDecimal margeConfiguree;
    private BigDecimal valeurMinCalculee;
    private BigDecimal valeurMaxCalculee;
    private LocalDateTime dateCalcul;

    public SeuilDynamiqueResponseDTO() {}

    public SeuilDynamiqueResponseDTO(UUID id, Long idPointMesure, String nomPointMesure, Metrique metrique,
                                    BigDecimal margeConfiguree, BigDecimal valeurMinCalculee,
                                    BigDecimal valeurMaxCalculee, LocalDateTime dateCalcul) {
        this.id = id;
        this.idPointMesure = idPointMesure;
        this.nomPointMesure = nomPointMesure;
        this.metrique = metrique;
        this.margeConfiguree = margeConfiguree;
        this.valeurMinCalculee = valeurMinCalculee;
        this.valeurMaxCalculee = valeurMaxCalculee;
        this.dateCalcul = dateCalcul;
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

    public BigDecimal getMargeConfiguree() {
        return margeConfiguree;
    }

    public void setMargeConfiguree(BigDecimal margeConfiguree) {
        this.margeConfiguree = margeConfiguree;
    }

    public BigDecimal getValeurMinCalculee() {
        return valeurMinCalculee;
    }

    public void setValeurMinCalculee(BigDecimal valeurMinCalculee) {
        this.valeurMinCalculee = valeurMinCalculee;
    }

    public BigDecimal getValeurMaxCalculee() {
        return valeurMaxCalculee;
    }

    public void setValeurMaxCalculee(BigDecimal valeurMaxCalculee) {
        this.valeurMaxCalculee = valeurMaxCalculee;
    }

    public LocalDateTime getDateCalcul() {
        return dateCalcul;
    }

    public void setDateCalcul(LocalDateTime dateCalcul) {
        this.dateCalcul = dateCalcul;
    }
}
