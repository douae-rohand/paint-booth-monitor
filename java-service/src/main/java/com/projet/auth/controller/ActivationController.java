package com.projet.auth.controller;

import com.projet.auth.dto.ActivationCompteDTO;
import com.projet.auth.service.SuperviseurAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/activation")
public class ActivationController {

    private final SuperviseurAdminService superviseurAdminService;

    public ActivationController(SuperviseurAdminService superviseurAdminService) {
        this.superviseurAdminService = superviseurAdminService;
    }

    @PostMapping
    public ResponseEntity<?> activerCompte(@RequestBody ActivationCompteDTO dto) {
        superviseurAdminService.activerCompte(dto);
        return ResponseEntity.ok().body("Compte activé avec succès — vous pouvez maintenant vous connecter");
    }
}
