package com.projet.config.model;

import com.projet.auth.model.Admin;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: config
 * SQL Table: configuration_plc
 *
 * Principle: immutable content after INSERT — aucun UPDATE de contenu.
 * A chaque changement de configuration par l'Admin, une nouvelle ligne est inseree.
 * Seuls les champs actif, dateActivation et dateDesactivation sont modifiables
 * apres creation pour gerer l'activation/desactivation.
 *
 * Written EXCLUSIVELY by java-service (Admin configuration).
 * Read by python-service (read-only) at startup to obtain connection parameters.
 */
@Entity
@Table(name = "configuration_plc")
public class ConfigurationPLC {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_configuration")
    private UUID idConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    // ── Champs de connexion PLC — figes apres insertion (jamais modifies) ─────

    @Column(name = "plc_ip", nullable = false, length = 45)
    private String plcIp;

    @Column(name = "plc_port", nullable = false)
    private Integer plcPort;

    @Column(name = "plc_rack", nullable = false)
    private Integer plcRack;

    @Column(name = "plc_slot", nullable = false)
    private Integer plcSlot;

    @Column(name = "plc_polling_interval_ms", nullable = false)
    private Integer plcPollingIntervalMs;

    // ── Champs de cycle de vie — modifiables apres insertion ──────────────────

    @Column(nullable = false)
    private boolean actif = false;

    /** Date d'insertion de la ligne. Jamais modifiee apres creation. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Date a laquelle cette configuration est devenue active. Null si jamais activee. */
    @Column(name = "date_activation")
    private LocalDateTime dateActivation;

    /** Date a laquelle cette configuration a ete desactivee. Null si encore active ou pas encore desactivee. */
    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    public ConfigurationPLC() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdConfiguration() { return idConfiguration; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public String getPlcIp() { return plcIp; }
    public void setPlcIp(String plcIp) { this.plcIp = plcIp; }

    public Integer getPlcPort() { return plcPort; }
    public void setPlcPort(Integer plcPort) { this.plcPort = plcPort; }

    public Integer getPlcRack() { return plcRack; }
    public void setPlcRack(Integer plcRack) { this.plcRack = plcRack; }

    public Integer getPlcSlot() { return plcSlot; }
    public void setPlcSlot(Integer plcSlot) { this.plcSlot = plcSlot; }

    public Integer getPlcPollingIntervalMs() { return plcPollingIntervalMs; }
    public void setPlcPollingIntervalMs(Integer plcPollingIntervalMs) { this.plcPollingIntervalMs = plcPollingIntervalMs; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }

    public LocalDateTime getDateActivation() { return dateActivation; }
    public void setDateActivation(LocalDateTime dateActivation) { this.dateActivation = dateActivation; }

    public LocalDateTime getDateDesactivation() { return dateDesactivation; }
    public void setDateDesactivation(LocalDateTime dateDesactivation) { this.dateDesactivation = dateDesactivation; }
}
