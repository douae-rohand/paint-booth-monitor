package com.projet.notifications.service;

import com.projet.notifications.dto.NotificationInAppDTO;
import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.Notification;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.repository.NotificationInAppRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service REST pour le bell icon — lecture et marquage des notifications IN_APP.
 *
 * Responsabilité unique : lire EnvoiNotification (canal=IN_APP), convertir en DTO,
 * gérer le marquage lu/non-lu.
 *
 * Le formatage du contenu textuel est délégué à {@link NotificationFormatter} —
 * source unique partagée avec NotificationPushService (toast WebSocket).
 * Garantit l'identité du texte entre le toast initial et la relecture du panel.
 */
@Service
public class NotificationInAppService {

    private final NotificationInAppRepository repo;
    private final NotificationFormatter formatter;

    public NotificationInAppService(
            NotificationInAppRepository repo,
            NotificationFormatter formatter
    ) {
        this.repo = repo;
        this.formatter = formatter;
    }

    /** Liste paginée des notifications IN_APP pour l'utilisateur courant. */
    public Page<NotificationInAppDTO> listerNotifications(UUID idSuperviseur, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.findBySuperviseurAndCanal(idSuperviseur, Canal.IN_APP, pageable)
                .map(this::toDTO);
    }

    /** Compteur non-lus pour le badge. */
    public long compterNonLues(UUID idSuperviseur) {
        return repo.countNonLues(idSuperviseur, Canal.IN_APP);
    }

    /** Marque une notification IN_APP comme lue. Vérifie l'appartenance au Superviseur. */
    @Transactional
    public void marquerLu(UUID idEnvoi, UUID idSuperviseur) {
        EnvoiNotification envoi = repo.findById(idEnvoi)
                .orElseThrow(() -> new IllegalArgumentException("EnvoiNotification introuvable : " + idEnvoi));

        if (!envoi.getSuperviseur().getIdSuperviseur().equals(idSuperviseur)) {
            throw new SecurityException("Accès refusé : notification " + idEnvoi
                    + " n'appartient pas à " + idSuperviseur);
        }

        if (!envoi.isLu()) {
            LocalDateTime now = LocalDateTime.now();
            envoi.setLu(true);
            envoi.setDateLecture(now);
            envoi.setUpdatedAt(now);
            repo.save(envoi);
        }
    }

    /** Marque toutes les notifications IN_APP non lues comme lues. */
    @Transactional
    public void marquerToutLu(UUID idSuperviseur) {
        repo.marquerToutLu(idSuperviseur, Canal.IN_APP, LocalDateTime.now());
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private NotificationInAppDTO toDTO(EnvoiNotification e) {
        Notification n = e.getNotification();
        return new NotificationInAppDTO(
                e.getIdEnvoi(),
                n.getIdNotification(),
                n.getTypeEvenement(),
                n.getTitre(),
                // Même formateur que NotificationPushService → texte identique entre toast et panel
                formatter.formaterContenuAffichage(n.getTypeEvenement(), n.getDonneesEvenement()),
                e.isLu(),
                n.getCreatedAt(),
                e.getDateLecture()
        );
    }
}
