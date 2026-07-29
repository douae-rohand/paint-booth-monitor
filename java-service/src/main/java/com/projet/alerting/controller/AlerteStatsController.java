package com.projet.alerting.controller;

import com.projet.alerting.dto.DetailJourAlertesDTO;
import com.projet.alerting.dto.HeatmapJourDTO;
import com.projet.alerting.dto.TopAlerteDTO;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.service.AlerteStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller pour les statistiques d'alertes.
 * Module: alerting
 * Endpoints: GET /api/alertes/top, GET /api/alertes/heatmap, GET /api/alertes/heatmap/jour
 */
@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteStatsController {

    private final AlerteStatsService alerteStatsService;

    /**
     * Récupère les top métriques en alerte sur une période.
     *
     * @param periode Période prédéfinie (optionnel)
     * @param dateDebut Date de début (optionnel, requis si periode="personnalise")
     * @param dateFin Date de fin (optionnel, requis si periode="personnalise")
     * @param pointMesureId ID du point de mesure (optionnel)
     * @param limit Nombre maximum de résultats (défaut 10)
     * @return Liste des top alertes
     */
    @GetMapping("/top")
    public ResponseEntity<List<TopAlerteDTO>> getTopAlertes(
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Long pointMesureId,
            @RequestParam(defaultValue = "10") int limit) {

        // Résoudre la période si fournie
        if (periode != null && !"personnalise".equals(periode)) {
            LocalDateTime now = LocalDateTime.now();
            dateFin = now;
            dateDebut = calculerDateDebut(periode, now);
        }

        List<TopAlerteDTO> result = alerteStatsService.getTopAlertes(dateDebut, dateFin, pointMesureId, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère les données de heatmap pour un mois donné.
     *
     * @param annee Année
     * @param mois Mois (1-12)
     * @param pointMesureId ID du point de mesure (optionnel)
     * @param metrique Métrique (optionnel)
     * @return Liste des données par jour du mois
     */
    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapJourDTO>> getHeatmapMois(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long pointMesureId,
            @RequestParam(required = false) Metrique metrique) {

        List<HeatmapJourDTO> result = alerteStatsService.getHeatmapMois(annee, mois, pointMesureId, metrique);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le détail des alertes pour un jour donné.
     *
     * @param date Date du jour (format YYYY-MM-DD)
     * @param pointMesureId ID du point de mesure (optionnel)
     * @return Détail des alertes du jour
     */
    @GetMapping("/heatmap/jour")
    public ResponseEntity<DetailJourAlertesDTO> getDetailJour(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long pointMesureId) {

        DetailJourAlertesDTO result = alerteStatsService.getDetailJour(date, pointMesureId);
        return ResponseEntity.ok(result);
    }

    /**
     * Calcule la date de début selon la période prédéfinie.
     *
     * @param periode Période ("24h", "7j", "30j", "3mois", "6mois", "1an")
     * @param now Date actuelle
     * @return Date de début calculée
     */
    private LocalDateTime calculerDateDebut(String periode, LocalDateTime now) {
        return switch (periode) {
            case "24h" -> now.minusHours(24);
            case "7j" -> now.minusDays(7);
            case "30j" -> now.minusDays(30);
            case "3mois" -> now.minusMonths(3);
            case "6mois" -> now.minusMonths(6);
            case "1an" -> now.minusYears(1);
            default -> now.minusHours(24);  // Défaut : 24h
        };
    }
}
