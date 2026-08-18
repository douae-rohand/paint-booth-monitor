package com.projet.notifications.controller;

import com.projet.auth.model.Superviseur;
import com.projet.notifications.dto.NotificationInAppDTO;
import com.projet.notifications.service.NotificationInAppService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints REST pour le bell icon — notifications IN_APP de l'utilisateur courant.
 *
 * GET  /api/notifications                     → liste paginée
 * GET  /api/notifications/non-lues/count      → compteur badge
 * PATCH /api/notifications/{id}/lu            → marquer une notification lue
 * PATCH /api/notifications/lu-tout            → tout marquer comme lu
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationInAppController {

    private final NotificationInAppService service;

    public NotificationInAppController(NotificationInAppService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationInAppDTO>> lister(
            @AuthenticationPrincipal Superviseur superviseur,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID id = superviseur.getIdSuperviseur();
        return ResponseEntity.ok(service.listerNotifications(id, page, size));
    }

    @GetMapping("/non-lues/count")
    public ResponseEntity<Map<String, Long>> compterNonLues(
            @AuthenticationPrincipal Superviseur superviseur
    ) {
        long count = service.compterNonLues(superviseur.getIdSuperviseur());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{idEnvoi}/lu")
    public ResponseEntity<Void> marquerLu(
            @PathVariable UUID idEnvoi,
            @AuthenticationPrincipal Superviseur superviseur
    ) {
        service.marquerLu(idEnvoi, superviseur.getIdSuperviseur());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lu-tout")
    public ResponseEntity<Void> marquerToutLu(
            @AuthenticationPrincipal Superviseur superviseur
    ) {
        service.marquerToutLu(superviseur.getIdSuperviseur());
        return ResponseEntity.noContent().build();
    }
}
