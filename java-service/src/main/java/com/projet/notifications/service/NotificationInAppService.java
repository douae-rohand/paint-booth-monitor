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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service REST pour le bell icon — lecture et marquage des notifications IN_APP.
 *
 * Règle d'affichage du panel :
 *   1. Toutes les non-lues (LIMIT 100 comme garde-fou technique uniquement).
 *   2. Les lues des 7 derniers jours, plafonnées à 5.
 *   3. Non-lues en premier (created_at DESC), lues récentes à la suite (created_at DESC).
 *
 * Le badge (countNonLues) reste exact et sans limite — pas concerné par cette règle.
 */
@Service
public class NotificationInAppService {

    /** Nombre de jours de rétention pour les notifications lues dans le panel. */
    private static final int NOTIFICATIONS_LUES_RETENTION_JOURS = 7;

    /** Nombre maximum de notifications lues affichées dans le panel. */
    private static final int NOTIFICATIONS_LUES_MAX_AFFICHEES = 5;

    private final NotificationInAppRepository repo;
    private final NotificationFormatter formatter;

    public NotificationInAppService(
            NotificationInAppRepository repo,
            NotificationFormatter formatter
    ) {
        this.repo = repo;
        this.formatter = formatter;
    }

    /**
     * Liste combinée pour le panel : toutes les non-lues + lues récentes.
     * Remplace l'ancienne méthode paginée — retourne une List, pas une Page.
     * Aucune troncature des non-lues (hors garde-fou technique à 100).
     */
    public List<NotificationInAppDTO> listerPourPanel(UUID idSuperviseur) {
        // 1. Toutes les non-lues (garde-fou : LIMIT 100 côté SQL)
        List<EnvoiNotification> nonLues = repo.findNonLuesPourPanel(idSuperviseur);

        // 2. Lues récentes (7 jours, max 5)
        LocalDateTime depuis = LocalDateTime.now().minusDays(NOTIFICATIONS_LUES_RETENTION_JOURS);
        List<EnvoiNotification> luesRecentes = repo.findLuesRecentesPourPanel(idSuperviseur, depuis);

        // 3. Combinaison : non-lues d'abord, lues récentes à la suite
        List<EnvoiNotification> combined = new ArrayList<>(nonLues.size() + luesRecentes.size());
        combined.addAll(nonLues);
        combined.addAll(luesRecentes);

        return combined.stream().map(this::toDTO).toList();
    }

    /**
     * Ancienne méthode paginée — conservée pour ne pas casser NotificationInAppController
     * si d'autres clients venaient à l'utiliser. Le controller est mis à jour pour
     * utiliser listerPourPanel() à la place.
     */
    public Page<NotificationInAppDTO> listerNotifications(UUID idSuperviseur, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.findBySuperviseurAndCanal(idSuperviseur, Canal.IN_APP, pageable)
                .map(this::toDTO);
    }

    /** Compteur non-lus pour le badge — exact, sans limite ni fenêtre de rétention. */
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
                formatter.formaterContenuAffichage(n.getTypeEvenement(), n.getDonneesEvenement()),
                e.isLu(),
                n.getCreatedAt(),
                e.getDateLecture()
        );
    }
}
