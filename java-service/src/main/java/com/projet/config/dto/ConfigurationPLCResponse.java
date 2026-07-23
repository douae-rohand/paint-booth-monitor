package com.projet.config.dto;

import com.projet.config.model.ConfigurationPLC;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour une configuration PLC.
 * Expose par tous les endpoints GET et les endpoints PATCH de
 * ConfigurationPLCController.
 */
public class ConfigurationPLCResponse {

    private UUID idConfiguration;
    private UUID idAdmin;
    private String plcIp;
    private Integer plcPort;
    private Integer plcRack;
    private Integer plcSlot;
    private Integer plcPollingIntervalMs;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateActivation;
    private LocalDateTime dateDesactivation;

    // -- Constructeurs ---------------------------------------------------------

    public ConfigurationPLCResponse() {}

    /**
     * Construit un DTO a partir de l'entite JPA.
     * Acces a admin.getIdAdmin() : l'association Admin est chargee en LAZY,
     * appeler ce mapping depuis un contexte transactionnel ou avec le proxy initialise.
     */
    public static ConfigurationPLCResponse from(ConfigurationPLC entity) {
        ConfigurationPLCResponse dto = new ConfigurationPLCResponse();
        dto.idConfiguration    = entity.getIdConfiguration();
        dto.idAdmin            = entity.getAdmin().getIdAdmin();
        dto.plcIp              = entity.getPlcIp();
        dto.plcPort            = entity.getPlcPort();
        dto.plcRack            = entity.getPlcRack();
        dto.plcSlot            = entity.getPlcSlot();
        dto.plcPollingIntervalMs = entity.getPlcPollingIntervalMs();
        dto.actif              = entity.isActif();
        dto.dateCreation       = entity.getDateCreation();
        dto.dateActivation     = entity.getDateActivation();
        dto.dateDesactivation  = entity.getDateDesactivation();
        return dto;
    }

    // -- Getters ---------------------------------------------------------------

    public UUID getIdConfiguration() { return idConfiguration; }
    public UUID getIdAdmin() { return idAdmin; }
    public String getPlcIp() { return plcIp; }
    public Integer getPlcPort() { return plcPort; }
    public Integer getPlcRack() { return plcRack; }
    public Integer getPlcSlot() { return plcSlot; }
    public Integer getPlcPollingIntervalMs() { return plcPollingIntervalMs; }
    public boolean isActif() { return actif; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateActivation() { return dateActivation; }
    public LocalDateTime getDateDesactivation() { return dateDesactivation; }
}
