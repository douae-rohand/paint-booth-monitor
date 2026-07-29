package com.projet.measures.controller;

import com.projet.measures.dto.PointMesureStatutDTO;
import com.projet.measures.service.StatutTempsReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour le statut temps réel des points de mesure.
 * Module: measures
 * Endpoint: GET /api/mesures/temps-reel
 */
@RestController
@RequestMapping("/api/mesures/temps-reel")
@RequiredArgsConstructor
public class StatutTempsReelController {

    private final StatutTempsReelService statutTempsReelService;

    /**
     * Récupère le statut temps réel de tous les points de mesure actifs.
     * Pas de paramètre - retourne l'état de tous les points.
     *
     * @return Liste des statuts de tous les points
     */
    @GetMapping
    public ResponseEntity<List<PointMesureStatutDTO>> getStatutTousPoints() {
        List<PointMesureStatutDTO> statuts = statutTempsReelService.getStatutTousPoints();
        return ResponseEntity.ok(statuts);
    }
}
