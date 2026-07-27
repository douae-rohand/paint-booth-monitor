package com.projet.alerting.dto;

import java.math.BigDecimal;

public class SeuilDynamiqueUpdateDTO {
    private BigDecimal margeConfiguree;

    public SeuilDynamiqueUpdateDTO() {}

    public SeuilDynamiqueUpdateDTO(BigDecimal margeConfiguree) {
        this.margeConfiguree = margeConfiguree;
    }

    public BigDecimal getMargeConfiguree() {
        return margeConfiguree;
    }

    public void setMargeConfiguree(BigDecimal margeConfiguree) {
        this.margeConfiguree = margeConfiguree;
    }
}
