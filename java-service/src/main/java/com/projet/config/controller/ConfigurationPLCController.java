package com.projet.config.controller;

import com.projet.auth.model.Superviseur;
import com.projet.config.dto.ConfigurationPLCRequest;
import com.projet.config.dto.ConfigurationPLCResponse;
import com.projet.config.service.ConfigurationPLCService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Module: config
 * Base path: /api/config/plc
 *
 * Tous les endpoints sont reserves au role ADMIN.
 * Pas de PUT ni de DELETE : la table configuration_plc est en append-only,
 * la mise a jour de contenu est remplacee par une nouvelle insertion (creerConfiguration).
 */
@RestController
@RequestMapping("/api/config/plc")
@Secured("ROLE_ADMIN")
public class ConfigurationPLCController {

    private final ConfigurationPLCService configurationPLCService;

    public ConfigurationPLCController(ConfigurationPLCService configurationPLCService) {
        this.configurationPLCService = configurationPLCService;
    }

    /**
     * GET /api/config/plc/active
     * Retourne la configuration PLC actuellement active.
     * Retourne 404 si aucune configuration n'est active.
     */
    @GetMapping("/active")
    public ResponseEntity<ConfigurationPLCResponse> getActive() {
        return ResponseEntity.ok(configurationPLCService.getConfigurationActive());
    }

    /**
     * GET /api/config/plc/history
     * Retourne l'historique complet des configurations PLC (de la plus recente a la plus ancienne).
     */
    @GetMapping("/history")
    public ResponseEntity<List<ConfigurationPLCResponse>> getHistory() {
        return ResponseEntity.ok(configurationPLCService.getHistorique());
    }

    /**
     * POST /api/config/plc
     * Cree une nouvelle configuration PLC et l'active immediatement.
     * La configuration active precedente est automatiquement desactivee.
     * Retourne 201 CREATED avec le DTO de la nouvelle configuration.
     */
    @PostMapping
    public ResponseEntity<ConfigurationPLCResponse> create(
            @RequestBody ConfigurationPLCRequest request,
            Authentication authentication) {

        Superviseur superviseur = (Superviseur) authentication.getPrincipal();
        UUID adminId = superviseur.getAdmin().getIdAdmin();

        ConfigurationPLCResponse response = configurationPLCService.creerConfiguration(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/config/plc/{id}/activer
     * Active la configuration identifiee par {id}.
     * Desactive automatiquement toute autre configuration active.
     */
    @PatchMapping("/{id}/activer")
    public ResponseEntity<ConfigurationPLCResponse> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(configurationPLCService.activerConfiguration(id));
    }

    /**
     * PATCH /api/config/plc/{id}/desactiver
     * Desactive la configuration identifiee par {id}.
     * Autorise meme si cela entraine l'absence de configuration active.
     */
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ConfigurationPLCResponse> desactiver(@PathVariable UUID id) {
        return ResponseEntity.ok(configurationPLCService.desactiverConfiguration(id));
    }
}
