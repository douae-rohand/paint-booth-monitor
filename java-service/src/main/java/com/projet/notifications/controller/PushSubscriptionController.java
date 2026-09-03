package com.projet.notifications.controller;

import com.projet.auth.model.Superviseur;
import com.projet.notifications.dto.PushSubscriptionRequestDTO;
import com.projet.notifications.dto.PushSubscriptionResponseDTO;
import com.projet.notifications.model.AbonnementPushNavigateur;
import com.projet.notifications.repository.AbonnementPushNavigateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour la gestion des abonnements Web Push (VAPID).
 * Module: notifications
 * Endpoints: GET /api/notifications/push/cle-publique, POST /api/notifications/push/abonnement, DELETE /api/notifications/push/abonnement
 */
@RestController
@RequestMapping("/api/notifications/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final AbonnementPushNavigateurRepository abonnementPushNavigateurRepository;

    @Value("${vapid.public-key:}")
    private String vapidPublicKey;

    /**
     * Retourne la clé publique VAPID nécessaire au frontend pour s'abonner.
     * Authentifié, tous rôles.
     *
     * @return Clé publique VAPID
     */
    @GetMapping("/cle-publique")
    public ResponseEntity<String> getClePublique() {
        return ResponseEntity.ok(vapidPublicKey);
    }

    /**
     * Crée ou met à jour un abonnement Web Push pour le superviseur courant.
     * Authentifié uniquement.
     *
     * @param request DTO contenant endpoint, cleP256dh, cleAuth
     * @param superviseur Superviseur authentifié
     * @return DTO de réponse avec l'abonnement créé/mis à jour
     */
    @PostMapping("/abonnement")
    public ResponseEntity<PushSubscriptionResponseDTO> creerOuMettreAJourAbonnement(
            @RequestBody PushSubscriptionRequestDTO request,
            @AuthenticationPrincipal Superviseur superviseur) {

        // Vérifier si un abonnement existe déjà pour cet endpoint
        java.util.Optional<AbonnementPushNavigateur> existing = abonnementPushNavigateurRepository.findByEndpoint(request.getEndpoint());

        AbonnementPushNavigateur abonnement;
        if (existing.isPresent()) {
            // Mettre à jour l'abonnement existant (changement de superviseur ou de clés)
            abonnement = existing.get();
            abonnement.setSuperviseur(superviseur);
            abonnement.setCleP256dh(request.getCleP256dh());
            abonnement.setCleAuth(request.getCleAuth());
            abonnement.setUserAgent(request.getUserAgent());
            abonnement = abonnementPushNavigateurRepository.save(abonnement);
        } else {
            // Créer un nouvel abonnement
            abonnement = new AbonnementPushNavigateur(
                    superviseur,
                    request.getEndpoint(),
                    request.getCleP256dh(),
                    request.getCleAuth(),
                    request.getUserAgent()
            );
            abonnement = abonnementPushNavigateurRepository.save(abonnement);
        }

        return ResponseEntity.ok(new PushSubscriptionResponseDTO(
                abonnement.getId(),
                abonnement.getEndpoint(),
                abonnement.getDateCreation()
        ));
    }

    /**
     * Supprime un abonnement Web Push pour le superviseur courant.
     * Authentifié uniquement. Vérifie l'appartenance avant suppression.
     *
     * @param endpoint URL du service de push à désabonner
     * @param superviseur Superviseur authentifié
     * @return 204 No Content si succès, 404 si non trouvé ou n'appartient pas au superviseur
     */
    @DeleteMapping("/abonnement")
    public ResponseEntity<Void> supprimerAbonnement(
            @RequestParam String endpoint,
            @AuthenticationPrincipal Superviseur superviseur) {

        java.util.Optional<AbonnementPushNavigateur> existing = abonnementPushNavigateurRepository.findByEndpoint(endpoint);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AbonnementPushNavigateur abonnement = existing.get();

        // Vérifier que l'abonnement appartient au superviseur courant
        if (!abonnement.getSuperviseur().getIdSuperviseur().equals(superviseur.getIdSuperviseur())) {
            return ResponseEntity.notFound().build();
        }

        abonnementPushNavigateurRepository.delete(abonnement);
        return ResponseEntity.noContent().build();
    }
}
