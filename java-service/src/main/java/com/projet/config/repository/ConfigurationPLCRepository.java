package com.projet.config.repository;

import com.projet.config.model.ConfigurationPLC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationPLCRepository extends JpaRepository<ConfigurationPLC, UUID> {

    /**
     * Retourne la configuration active.
     * Invariant metier : il ne doit jamais exister plus d'une configuration active en base.
     */
    Optional<ConfigurationPLC> findByActifTrue();

    /**
     * Retourne l'historique complet des configurations, de la plus recente a la plus ancienne.
     */
    List<ConfigurationPLC> findAllByOrderByDateCreationDesc();

    Optional<ConfigurationPLC> findFirstByPlcIpAndPlcPortAndPlcRackAndPlcSlotAndPlcPollingIntervalMs(
            String plcIp,
            Integer plcPort,
            Integer plcRack,
            Integer plcSlot,
            Integer plcPollingIntervalMs);
}
