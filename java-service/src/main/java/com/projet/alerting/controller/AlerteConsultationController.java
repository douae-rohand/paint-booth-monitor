package com.projet.alerting.controller;

import com.projet.alerting.dto.AlerteDTO;
import com.projet.alerting.service.AlerteConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller pour la consultation des alertes.
 * Module: alerting
 * Endpoints: GET /api/alertes, GET /api/alertes/actives
 */
@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteConsultationController {

    private final AlerteConsultationService alerteConsultationService;

    /**
     * Récupère l'historique des alertes avec filtres optionnels et pagination.
     *
     * @param statut Filtre par statut (optionnel)
     * @param typeAlerte Filtre par type d'alerte (optionnel)
     * @param severite Filtre par sévérité (optionnel)
     * @param idPointMesure Filtre par ID du point de mesure (optionnel)
     * @param dateDebut Filtre par date de début (optionnel)
     * @param dateFin Filtre par date de fin (optionnel)
     * @param pageable Pagination (défaut: page 0, size 20)
     * @return Page d'AlerteDTO
     */
    @GetMapping
    public ResponseEntity<Page<AlerteDTO>> getHistoriqueAlertes(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String typeAlerte,
            @RequestParam(required = false) String severite,
            @RequestParam(required = false) Long idPointMesure,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AlerteDTO> result = alerteConsultationService.getHistoriqueAlertes(
                statut,
                typeAlerte,
                severite,
                idPointMesure,
                dateDebut,
                dateFin,
                pageable
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Récupère toutes les alertes actives (sans pagination).
     *
     * @return Liste d'AlerteDTO
     */
    @GetMapping("/actives")
    public ResponseEntity<List<AlerteDTO>> getAlertesActives() {
        List<AlerteDTO> result = alerteConsultationService.getAlertesActives();
        return ResponseEntity.ok(result);
    }
}
