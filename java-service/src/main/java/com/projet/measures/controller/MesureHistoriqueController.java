package com.projet.measures.controller;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.dto.MesureHistoriqueResponseDTO;
import com.projet.measures.service.MesureHistoriqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller pour l'historique des mesures.
 * Module: measures
 * Endpoint: GET /api/mesures/historique
 */
@RestController
@RequestMapping("/api/mesures/historique")
@RequiredArgsConstructor
public class MesureHistoriqueController {

    private final MesureHistoriqueService mesureHistoriqueService;

    /**
     * Récupère l'historique des mesures pour un point de mesure et une métrique sur une période.
     *
     * @param pointMesureId ID du point de mesure (requis)
     * @param metrique Métrique demandée (requis)
     * @param dateDebut Date de début de la période (requis)
     * @param dateFin Date de fin de la période (requis)
     * @return MesureHistoriqueResponseDTO avec les points et le seuil absolu actif
     */
    @GetMapping
    public ResponseEntity<MesureHistoriqueResponseDTO> getHistorique(
            @RequestParam Long pointMesureId,
            @RequestParam Metrique metrique,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {

        MesureHistoriqueResponseDTO response = mesureHistoriqueService.getHistorique(
                pointMesureId, metrique, dateDebut, dateFin);

        return ResponseEntity.ok(response);
    }
}
