package com.projet.config.model;

import com.projet.auth.model.Admin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: config
 * SQL Table: configuration_plc
 *
 * Configures connection settings for the PLC (IP, rack, slot, polling interval).
 * Written by Java (Admin configuration), read by Python.
 */
@Entity
@Table(name = "configuration_plc")
@Getter
@Setter
public class ConfigurationPLC {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_configuration")
    private UUID idConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    @Column(name = "plc_ip", nullable = false, length = 45)
    private String plcIp;

    @Column(name = "plc_rack", nullable = false)
    private Integer plcRack;

    @Column(name = "plc_slot", nullable = false)
    private Integer plcSlot;

    @Column(name = "plc_polling_interval_ms", nullable = false)
    private Integer plcPollingIntervalMs;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ConfigurationPLC() {}
}
