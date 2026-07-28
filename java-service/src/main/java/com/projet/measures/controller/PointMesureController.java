package com.projet.measures.controller;

import com.projet.measures.dto.PointMesureResponse;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.PointMesureRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller pour la gestion des points de mesure.
 * Expose les endpoints pour lister les points de mesure physiques (cabine, zones d'étuve).
 */
@RestController
@RequestMapping("/api/point-mesures")
public class PointMesureController {

    private final PointMesureRepository pointMesureRepository;

    public PointMesureController(PointMesureRepository pointMesureRepository) {
        this.pointMesureRepository = pointMesureRepository;
    }

    /**
     * GET /api/point-mesures
     * Retourne tous les points de mesure actifs.
     *
     * @return Liste des points de mesure actifs
     */
    @GetMapping
    public ResponseEntity<List<PointMesureResponse>> getAllPointMesures() {
        List<PointMesure> pointMesures = pointMesureRepository.findAllByActifTrue();
        List<PointMesureResponse> responses = pointMesures.stream()
                .map(PointMesureResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
