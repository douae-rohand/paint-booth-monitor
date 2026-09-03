package com.projet.reports.controller;

import com.projet.auth.model.Superviseur;
import com.projet.audit.annotation.Audite;
import com.projet.audit.model.enums.ActionAudit;
import com.projet.reports.dto.RapportGenerationRequestDTO;
import com.projet.reports.dto.RapportPDFResponseDTO;
import com.projet.reports.model.RapportPDF;
import com.projet.reports.model.enums.TypeRapport;
import com.projet.reports.repository.RapportPdfRepository;
import com.projet.reports.service.RapportGenerationService;
import com.projet.reports.storage.MinioStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller pour la génération et le téléchargement de rapports PDF.
 * Module: reports
 * Endpoints: POST /api/rapports, GET /api/rapports, GET /api/rapports/{id}, GET /api/rapports/{id}/telecharger
 */
@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
public class ReportController {

    private final RapportGenerationService rapportGenerationService;
    private final RapportPdfRepository rapportPdfRepository;
    private final MinioStorageService minioStorageService;

    /**
     * Génère un rapport PDF de manière synchrone.
     *
     * @param request DTO contenant idPointMesure, dateDebut, dateFin, typeRapport
     * @param superviseur Superviseur authentifié
     * @return RapportPDF avec son statut final (TERMINE ou ECHEC)
     */
    @PostMapping
    public ResponseEntity<RapportPDF> genererRapport(
            @RequestBody RapportGenerationRequestDTO request,
            @AuthenticationPrincipal Superviseur superviseur) {

        RapportPDF rapport = rapportGenerationService.genererRapport(
                request.getIdPointMesure(),
                request.getDateDebut(),
                request.getDateFin(),
                request.getTypeRapport() != null ? request.getTypeRapport() : TypeRapport.PERSONNALISE,
                superviseur
        );

        return ResponseEntity.ok(rapport);
    }

    /**
     * Liste les rapports PDF avec pagination.
     * Chaque utilisateur (Superviseur et Admin) voit UNIQUEMENT ses propres rapports.
     *
     * @param superviseur Superviseur authentifié
     * @param pageable Pagination
     * @return Page de RapportPDF
     */
    @GetMapping
    public ResponseEntity<Page<RapportPDFResponseDTO>> listerRapports(
            @AuthenticationPrincipal Superviseur superviseur,
            Pageable pageable) {

        Page<RapportPDFResponseDTO> result = rapportPdfRepository
                .findBySuperviseurIdSuperviseur(superviseur.getIdSuperviseur(), pageable)
                .map(RapportPDFResponseDTO::from);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère le détail d'un rapport PDF.
     * Un utilisateur peut consulter uniquement ses propres rapports.
     *
     * @param id ID du rapport
     * @param superviseur Superviseur authentifié
     * @return RapportPDF
     */
    @GetMapping("/{id}")
    public ResponseEntity<RapportPDFResponseDTO> getRapport(
            @PathVariable UUID id,
            @AuthenticationPrincipal Superviseur superviseur) {

        RapportPDF rapport = rapportPdfRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapport non trouvé: " + id));

        if (!rapport.getSuperviseur().getIdSuperviseur().equals(superviseur.getIdSuperviseur())) {
            throw new AccessDeniedException("Accès non autorisé à ce rapport");
        }

        return ResponseEntity.ok(RapportPDFResponseDTO.from(rapport));
    }

    /**
     * Télécharge le fichier PDF d'un rapport.
     * Un utilisateur peut télécharger uniquement ses propres rapports.
     *
     * @param id ID du rapport
     * @param superviseur Superviseur authentifié
     * @param response HttpServletResponse pour le streaming du fichier
     */
    @Audite(ActionAudit.TELECHARGEMENT_RAPPORT)
    @GetMapping("/{id}/telecharger")
    public void telechargerRapport(
            @PathVariable UUID id,
            @AuthenticationPrincipal Superviseur superviseur,
            HttpServletResponse response) throws IOException {

        RapportPDF rapport = rapportPdfRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapport non trouvé: " + id));

        // Vérifier le propriétaire (403 via GlobalExceptionHandler si non autorisé)
        if (!rapport.getSuperviseur().getIdSuperviseur().equals(superviseur.getIdSuperviseur())) {
            throw new AccessDeniedException("Accès non autorisé à ce rapport");
        }

        if (rapport.getStatutGeneration() != com.projet.reports.model.enums.StatutGeneration.TERMINE) {
            throw new IllegalArgumentException("Le rapport n'est pas encore terminé");
        }

        if (rapport.getObjetMinioStorageKey() == null) {
            throw new IllegalArgumentException("Fichier PDF non disponible");
        }

        // Récupérer le fichier depuis MinIO
        InputStream inputStream;
        try {
            inputStream = minioStorageService.download(rapport.getObjetMinioStorageKey());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Erreur lors du téléchargement depuis MinIO : " + e.getMessage(), e);
        }

        // Configurer la réponse HTTP
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + rapport.getNomFichier() + "\"");

        // Stream le fichier
        OutputStream outputStream = response.getOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
    }
}
