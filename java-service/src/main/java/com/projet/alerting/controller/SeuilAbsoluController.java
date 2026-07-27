package com.projet.alerting.controller;

import com.projet.alerting.dto.SeuilAbsoluCreateDTO;
import com.projet.alerting.dto.SeuilAbsoluResponseDTO;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.service.SeuilAbsoluService;
import com.projet.auth.model.Superviseur;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seuils/absolus")
public class SeuilAbsoluController {

    private final SeuilAbsoluService seuilAbsoluService;

    public SeuilAbsoluController(SeuilAbsoluService seuilAbsoluService) {
        this.seuilAbsoluService = seuilAbsoluService;
    }

    @GetMapping("/active")
    public ResponseEntity<SeuilAbsoluResponseDTO> getActive(
            @RequestParam Long pointMesureId,
            @RequestParam Metrique metrique) {
        return ResponseEntity.ok(seuilAbsoluService.getActive(pointMesureId, metrique));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SeuilAbsoluResponseDTO>> getHistory(
            @RequestParam Long pointMesureId,
            @RequestParam Metrique metrique) {
        return ResponseEntity.ok(seuilAbsoluService.getHistory(pointMesureId, metrique));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeuilAbsoluResponseDTO> create(
            @RequestBody SeuilAbsoluCreateDTO dto,
            Authentication authentication) {
        Superviseur superviseur = (Superviseur) authentication.getPrincipal();
        UUID adminId = superviseur.getAdmin().getIdAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(seuilAbsoluService.creer(dto, adminId));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeuilAbsoluResponseDTO> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(seuilAbsoluService.activer(id));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeuilAbsoluResponseDTO> desactiver(@PathVariable UUID id) {
        return ResponseEntity.ok(seuilAbsoluService.desactiver(id));
    }
}
