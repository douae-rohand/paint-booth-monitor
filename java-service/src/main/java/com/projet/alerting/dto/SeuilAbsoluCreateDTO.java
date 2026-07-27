package com.projet.alerting.dto;

import com.projet.alerting.model.enums.Metrique;
import java.math.BigDecimal;

public class SeuilAbsoluCreateDTO {
    private Long idPointMesure;
    private Metrique metrique;
    private BigDecimal valeurMin;
    private BigDecimal valeurMax;

    public SeuilAbsoluCreateDTO() {}

    public SeuilAbsoluCreateDTO(Long idPointMesure, Metrique metrique, BigDecimal valeurMin, BigDecimal valeurMax) {
        this.idPointMesure = idPointMesure;
        this.metrique = metrique;
        this.valeurMin = valeurMin;
        this.valeurMax = valeurMax;
    }

    public Long getIdPointMesure() {
        return idPointMesure;
    }

    public void setIdPointMesure(Long idPointMesure) {
        this.idPointMesure = idPointMesure;
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
}
