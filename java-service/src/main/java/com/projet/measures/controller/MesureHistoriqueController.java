package com.projet.measures.controller;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.dto.MesureHistoriqueResponseDTO;
import com.projet.measures.model.enums.Granularite;
import com.projet.measures.service.MesureHistoriqueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller pour l'historique des mesures avec agrégation par granularité.
 * Module: measures
 * Endpoint: GET /api/mesures/historique
 */
@RestController
@RequestMapping("/api/mesures/historique")
@RequiredArgsConstructor
@Slf4j
public class MesureHistoriqueController {

    private final MesureHistoriqueService mesureHistoriqueService;

    /**
     * Récupère l'historique des mesures pour un point de mesure et une métrique sur une période.
     *
     * @param pointMesureId ID du point de mesure (requis)
     * @param metrique Métrique demandée (requis)
     * @param periode Période prédéfinie (24h, 7j, 30j, 6mois, 1an, personnalise)
     * @param dateDebut Date de début de la période (requis)
     * @param dateFin Date de fin de la période (requis)
     * @param granularite Granularité demandée (optionnel, uniquement utilisé pour periode=7j)
     * @return MesureHistoriqueResponseDTO avec les points agrégés et le seuil absolu actif
     */
    @GetMapping
    public ResponseEntity<MesureHistoriqueResponseDTO> getHistorique(
            @RequestParam Long pointMesureId,
            @RequestParam Metrique metrique,
            @RequestParam String periode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Granularite granularite) {

        // Validation défensive : ignorer silencieusement granularite si periode != "7j"
        if (granularite != null && !"7j".equals(periode)) {
            log.debug("Paramètre granularite ignoré car période n'est pas '7j' (période: {}, granularite: {})", periode, granularite);
            granularite = null;
        }

        MesureHistoriqueResponseDTO response = mesureHistoriqueService.getHistorique(
                pointMesureId, metrique, periode, dateDebut, dateFin, granularite);

        return ResponseEntity.ok(response);
    }
}
