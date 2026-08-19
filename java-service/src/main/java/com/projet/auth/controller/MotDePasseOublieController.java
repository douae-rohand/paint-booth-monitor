package com.projet.auth.controller;

import com.projet.auth.dto.DemandeReinitialisationDTO;
import com.projet.auth.dto.ReinitialisationMotDePasseDTO;
import com.projet.auth.service.MotDePasseOublieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class MotDePasseOublieController {

    private final MotDePasseOublieService motDePasseOublieService;

    public MotDePasseOublieController(MotDePasseOublieService motDePasseOublieService) {
        this.motDePasseOublieService = motDePasseOublieService;
    }

    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<?> demanderReinitialisation(@RequestBody DemandeReinitialisationDTO dto) {
        motDePasseOublieService.demanderReinitialisation(dto);
        // Message générique renvoyé indépendamment de l'existence du compte pour éviter le data-mining d'emails.
        return ResponseEntity.ok().body("Si ce compte existe, un email contenant un lien de réinitialisation a été envoyé.");
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<?> reinitialiserMotDePasse(@RequestBody ReinitialisationMotDePasseDTO dto) {
        motDePasseOublieService.reinitialiserMotDePasse(dto);
        return ResponseEntity.ok().body("Votre mot de passe a été réinitialisé avec succès.");
    }
}
