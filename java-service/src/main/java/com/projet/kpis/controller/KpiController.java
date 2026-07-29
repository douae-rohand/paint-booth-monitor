package com.projet.kpis.controller;

import com.projet.alerting.model.enums.Metrique;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.kpis.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller pour les KPIs.
 * Module: kpis
 * Endpoint: GET /api/kpis
 */
@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    /**
     * Récupère les KPIs.
     * - Si pointMesureId ET metrique fournis : retourne les KPIs scopés à cette paire
     * - Si absents : retourne uniquement les KPIs globaux
     *
     * @param pointMesureId ID du point de mesure (optionnel)
     * @param metrique Métrique (optionnel)
     * @param dateDebut Date de début de la période (optionnel, requis si point+métrique fournis)
     * @param dateFin Date de fin de la période (optionnel, requis si point+métrique fournis)
     * @return KpiResponseDTO avec les KPIs
     */
    @GetMapping
    public ResponseEntity<KpiResponseDTO> getKpis(
            @RequestParam(required = false) Long pointMesureId,
            @RequestParam(required = false) Metrique metrique,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {

        KpiResponseDTO response;

        if (pointMesureId != null && metrique != null) {
            // Scope point+métrique fourni : calculer les KPIs scopés
            if (dateDebut == null || dateFin == null) {
                throw new IllegalArgumentException("dateDebut et dateFin sont requis lorsque pointMesureId et metrique sont fournis");
            }
            response = kpiService.getKpisParPoint(pointMesureId, metrique, dateDebut, dateFin);
        } else {
            // Scope global : retourner uniquement les KPIs globaux
            response = kpiService.getKpisGlobaux();
        }

        return ResponseEntity.ok(response);
    }
}
