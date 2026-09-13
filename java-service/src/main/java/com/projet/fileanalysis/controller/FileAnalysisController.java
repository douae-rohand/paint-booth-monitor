package com.projet.fileanalysis.controller;

import com.projet.fileanalysis.dto.FileAnalysisResponseDTO;
import com.projet.fileanalysis.service.FileAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller pour l'import et l'analyse de fichiers CSV externes.
 * Module: fileanalysis
 * Endpoint: POST /api/analyse-fichier
 */
@RestController
@RequestMapping("/api/analyse-fichier")
@RequiredArgsConstructor
@Slf4j
public class FileAnalysisController {

    private final FileAnalysisService fileAnalysisService;

    /**
     * Reçoit un fichier CSV de mesures externes, l'analyse, agrège les données
     * selon la granularité calculée et retourne le résultat pour affichage dans un graphe.
     *
     * @param file Fichier CSV téléversé (multipart/form-data)
     * @return FileAnalysisResponseDTO avec métriques, points agrégés et statistiques
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileAnalysisResponseDTO> analyserFichier(
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/analyse-fichier - Réception fichier: originalFilename={}, size={} octets",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);

        FileAnalysisResponseDTO response = fileAnalysisService.analyserFichier(file);

        log.info("Analyse de fichier terminée avec succès: labelMetrique='{}', granularite={}, pointsAgreges={}, lignesValides={}, lignesIgnorees={}",
                response.getLabelMetrique(), response.getGranularite(),
                response.getPoints() != null ? response.getPoints().size() : 0,
                response.getLignesValides(), response.getLignesIgnorees());

        return ResponseEntity.ok(response);
    }
}
