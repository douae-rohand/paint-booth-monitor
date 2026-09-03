package com.projet.audit.controller;

import com.projet.audit.dto.LogAuditResponseDTO;
import com.projet.audit.model.enums.ActionAudit;
import com.projet.audit.service.LogAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la consultation des logs d'audit.
 * Réservé au rôle ADMIN.
 * Module: audit
 * Endpoint: GET /api/admin/audit
 */
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditAdminController {

    private final LogAuditService logAuditService;

    public AuditAdminController(LogAuditService logAuditService) {
        this.logAuditService = logAuditService;
    }

    /**
     * Liste les logs d'audit avec filtres optionnels et pagination.
     *
     * @param idSuperviseur Filtre par superviseur (optionnel)
     * @param actions       Filtre par liste de types d'action, ex: ?actions=CONNEXION&actions=DECONNEXION
     *                      (optionnel — liste vide = pas de filtre)
     * @param dateDebut     Filtre : date de début (optionnel, format ISO 8601)
     * @param dateFin       Filtre : date de fin (optionnel, format ISO 8601)
     * @param pageable      Pagination (page, size, sort)
     * @return Page de LogAuditResponseDTO
     */
    @GetMapping
    public ResponseEntity<Page<LogAuditResponseDTO>> listerLogs(
            @RequestParam(required = false) UUID idSuperviseur,
            @RequestParam(required = false) List<ActionAudit> actions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            Pageable pageable) {

        Page<LogAuditResponseDTO> result = logAuditService.listerLogs(
                idSuperviseur, actions, dateDebut, dateFin, pageable);

        return ResponseEntity.ok(result);
    }
}
