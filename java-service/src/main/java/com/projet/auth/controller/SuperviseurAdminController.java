package com.projet.auth.controller;

import com.projet.auth.dto.*;
import com.projet.auth.service.SuperviseurAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/superviseurs")
@PreAuthorize("hasRole('ADMIN')")
public class SuperviseurAdminController {

    private final SuperviseurAdminService superviseurAdminService;

    public SuperviseurAdminController(SuperviseurAdminService superviseurAdminService) {
        this.superviseurAdminService = superviseurAdminService;
    }

    @GetMapping
    public ResponseEntity<Page<SuperviseurListItemDTO>> lister(
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) Boolean compteActive,
            @RequestParam(required = false) Boolean inclureAdmin,
            Pageable pageable) {
        return ResponseEntity.ok(superviseurAdminService.lister(actif, compteActive, inclureAdmin, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperviseurResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(superviseurAdminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SuperviseurResponseDTO> creer(@RequestBody SuperviseurCreateDTO dto) {
        return ResponseEntity.ok(superviseurAdminService.creer(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuperviseurResponseDTO> modifier(
            @PathVariable UUID id,
            @RequestBody SuperviseurUpdateDTO dto) {
        return ResponseEntity.ok(superviseurAdminService.modifier(id, dto));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable UUID id) {
        superviseurAdminService.desactiver(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<Void> activer(@PathVariable UUID id) {
        superviseurAdminService.activer(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/renvoyer-activation")
    public ResponseEntity<SuperviseurResponseDTO> renvoyerActivation(@PathVariable UUID id) {
        return ResponseEntity.ok(superviseurAdminService.renvoyerActivation(id));
    }
}
