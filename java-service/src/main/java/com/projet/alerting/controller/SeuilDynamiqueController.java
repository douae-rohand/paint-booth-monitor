package com.projet.alerting.controller;

import com.projet.alerting.dto.SeuilDynamiqueCreateDTO;
import com.projet.alerting.dto.SeuilDynamiqueResponseDTO;
import com.projet.alerting.dto.SeuilDynamiqueUpdateDTO;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.service.SeuilDynamiqueService;
import com.projet.auth.model.Superviseur;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seuils/dynamiques")
public class SeuilDynamiqueController {

    private final SeuilDynamiqueService seuilDynamiqueService;

    public SeuilDynamiqueController(SeuilDynamiqueService seuilDynamiqueService) {
        this.seuilDynamiqueService = seuilDynamiqueService;
    }

    @GetMapping
    public ResponseEntity<SeuilDynamiqueResponseDTO> get(
            @RequestParam Long pointMesureId,
            @RequestParam Metrique metrique) {
        return ResponseEntity.ok(seuilDynamiqueService.get(pointMesureId, metrique));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeuilDynamiqueResponseDTO> create(
            @RequestBody SeuilDynamiqueCreateDTO dto,
            Authentication authentication) {
        Superviseur superviseur = (Superviseur) authentication.getPrincipal();
        UUID adminId = superviseur.getAdmin().getIdAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(seuilDynamiqueService.creer(dto, adminId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeuilDynamiqueResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody SeuilDynamiqueUpdateDTO dto) {
        return ResponseEntity.ok(seuilDynamiqueService.modifierMarge(id, dto));
    }
}
