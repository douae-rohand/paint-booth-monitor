package com.projet.measures.controller;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.dto.MesureCabineDTO;
import com.projet.measures.dto.MesureEtuveDTO;
import com.projet.measures.dto.MesureHistoriqueResponseDTO;
import com.projet.measures.model.enums.Granularite;
import com.projet.measures.service.MesureExportService;
import com.projet.measures.service.MesureHistoriqueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final MesureExportService mesureExportService;

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

    /**
     * Récupère l'historique des mesures de la cabine avec pivot température/humidité par cycle.
     *
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement (défaut: false)
     * @param pageable Pagination (défaut: page 0, size 10)
     * @return Page de MesureCabineDTO
     */
    @GetMapping("/cabine")
    public ResponseEntity<Page<MesureCabineDTO>> getHistoriqueCabine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "false") boolean seulementDepassements,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("GET /api/mesures/historique/cabine - dateDebut={}, dateFin={}, seulementDepassements={}, pageable={}",
                dateDebut, dateFin, seulementDepassements, pageable);

        Page<MesureCabineDTO> response = mesureHistoriqueService.getHistoriqueCabine(
                dateDebut, dateFin, seulementDepassements, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère l'historique des mesures de l'étuve par zone.
     *
     * @param zone Nom de la zone (optionnel, "ZONE_1".."ZONE_5" ou null pour toutes zones)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement (défaut: false)
     * @param pageable Pagination (défaut: page 0, size 10)
     * @return Page de MesureEtuveDTO
     */
    @GetMapping("/etuve")
    public ResponseEntity<Page<MesureEtuveDTO>> getHistoriqueEtuve(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "false") boolean seulementDepassements,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("GET /api/mesures/historique/etuve - zone={}, dateDebut={}, dateFin={}, seulementDepassements={}, pageable={}",
                zone, dateDebut, dateFin, seulementDepassements, pageable);

        Page<MesureEtuveDTO> response = mesureHistoriqueService.getHistoriqueEtuve(
                zone, dateDebut, dateFin, seulementDepassements, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Exporte l'historique des mesures de la cabine.
     *
     * @param format Format d'export (csv, pdf ou xlsx)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @return Fichier CSV, PDF ou Excel
     */
    @GetMapping("/cabine/export")
    public ResponseEntity<byte[]> exportHistoriqueCabine(
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "false") boolean seulementDepassements) {

        log.info("GET /api/mesures/historique/cabine/export - format={}, dateDebut={}, dateFin={}, seulementDepassements={}",
                format, dateDebut, dateFin, seulementDepassements);

        byte[] data;
        String filename;
        String contentType;

        if ("csv".equalsIgnoreCase(format)) {
            data = mesureExportService.exportCabineCSV(dateDebut, dateFin, seulementDepassements);
            filename = "mesures_cabine_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else if ("pdf".equalsIgnoreCase(format)) {
            data = mesureExportService.exportCabinePDF(dateDebut, dateFin, seulementDepassements);
            filename = "mesures_cabine_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";
            contentType = "application/pdf";
        } else if ("xlsx".equalsIgnoreCase(format)) {
            data = mesureExportService.exportCabineExcel(dateDebut, dateFin, seulementDepassements);
            filename = "mesures_cabine_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            throw new IllegalArgumentException("Format non supporté: " + format + ". Utilisez 'csv', 'pdf' ou 'xlsx'.");
        }

        if (data.length == 0) {
            return ResponseEntity.noContent().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Exporte l'historique des mesures de l'étuve.
     *
     * @param format Format d'export (csv, pdf ou xlsx)
     * @param zone Nom de la zone (optionnel)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @return Fichier CSV, PDF ou Excel
     */
    @GetMapping("/etuve/export")
    public ResponseEntity<byte[]> exportHistoriqueEtuve(
            @RequestParam String format,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "false") boolean seulementDepassements) {

        log.info("GET /api/mesures/historique/etuve/export - format={}, zone={}, dateDebut={}, dateFin={}, seulementDepassements={}",
                format, zone, dateDebut, dateFin, seulementDepassements);

        byte[] data;
        String filename;
        String contentType;

        if ("csv".equalsIgnoreCase(format)) {
            data = mesureExportService.exportEtuveCSV(zone, dateDebut, dateFin, seulementDepassements);
            filename = "mesures_etuve_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else if ("pdf".equalsIgnoreCase(format)) {
            data = mesureExportService.exportEtuvePDF(zone, dateDebut, dateFin, seulementDepassements);
            filename = "mesures_etuve_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";
            contentType = "application/pdf";
        } else if ("xlsx".equalsIgnoreCase(format)) {
            data = mesureExportService.exportEtuveExcel(zone, dateDebut, dateFin, seulementDepassements);
            filename = "mesures_etuve_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            throw new IllegalArgumentException("Format non supporté: " + format + ". Utilisez 'csv', 'pdf' ou 'xlsx'.");
        }

        if (data.length == 0) {
            return ResponseEntity.noContent().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
