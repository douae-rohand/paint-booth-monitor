package com.projet.config.dto;

/**
 * DTO de creation d'une configuration PLC.
 * Transmis par le frontend dans le corps d'un POST /api/config/plc.
 *
 * Champs obligatoires : ip, rack, slot, intervallePolling.
 * Champ optionnel : port (valeur par defaut 102 appliquee dans le service si absent).
 */
public class ConfigurationPLCRequest {

    /** Adresse IP de l'automate (format IPv4 obligatoire). */
    private String ip;

    /**
     * Port TCP de l'automate (1-65535).
     * Optionnel a€” le service applique le port par defaut 102 (Snap7/ISO-TSAP) si null.
     */
    private Integer port;

    /** Numero de rack de l'automate (entier >= 0). */
    private Integer rack;

    /** Numero de slot de l'automate (entier >= 0). */
    private Integer slot;

    /** Intervalle de polling PLC en millisecondes (entier strictement positif). */
    private Integer intervallePolling;

    // -- Constructeurs ---------------------------------------------------------

    public ConfigurationPLCRequest() {}

    // -- Getters / Setters -----------------------------------------------------

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public Integer getRack() { return rack; }
    public void setRack(Integer rack) { this.rack = rack; }

    public Integer getSlot() { return slot; }
    public void setSlot(Integer slot) { this.slot = slot; }

    public Integer getIntervallePolling() { return intervallePolling; }
    public void setIntervallePolling(Integer intervallePolling) { this.intervallePolling = intervallePolling; }
}
