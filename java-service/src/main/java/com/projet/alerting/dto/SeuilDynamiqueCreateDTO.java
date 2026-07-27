package com.projet.alerting.dto;

import com.projet.alerting.model.enums.Metrique;
import java.math.BigDecimal;

public class SeuilDynamiqueCreateDTO {
    private Long idPointMesure;
    private Metrique metrique;
    private BigDecimal margeConfiguree;

    public SeuilDynamiqueCreateDTO() {}

    public SeuilDynamiqueCreateDTO(Long idPointMesure, Metrique metrique, BigDecimal margeConfiguree) {
        this.idPointMesure = idPointMesure;
        this.metrique = metrique;
        this.margeConfiguree = margeConfiguree;
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

    public BigDecimal getMargeConfiguree() {
        return margeConfiguree;
    }

    public void setMargeConfiguree(BigDecimal margeConfiguree) {
        this.margeConfiguree = margeConfiguree;
    }
}
