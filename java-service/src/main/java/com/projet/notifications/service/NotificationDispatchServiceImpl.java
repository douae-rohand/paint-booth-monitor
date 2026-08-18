package com.projet.notifications.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.Severite;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.service.AlerteBroadcastService;
import com.projet.auth.model.Admin;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.AdminRepository;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.measures.model.Mesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.Notification;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.StatutEnvoi;
import com.projet.notifications.model.enums.TypeEvenement;
import com.projet.notifications.repository.EnvoiNotificationRepository;
import com.projet.notifications.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation du service de dispatch des notifications personnelles multicanal.
 *
 * Responsabilités (après refactoring final) :
 *  1. Résolution des données brutes de l'événement (Alerte, PointMesure, Superviseur...).
 *  2. Construction du titre court par type d'événement.
 *  3. Persistance : 1 Notification (titre + donnees_evenement JSONB) + N EnvoiNotification.
 *  4. Délégation du push IN_APP à NotificationPushService.
 *     Canal EMAIL → EN_ATTENTE, traité par EmailWorkerService via @Scheduled.
 *
 * Ce service ne connaît plus SimpMessagingTemplate ni KpiService :
 *  - Le broadcast global (/topic/alertes, /topic/kpis) est délégué à AlerteBroadcastService,
 *    appelé séparément par PostgresNotificationListener.
 *  - Le push personnel (/user/queue/notifications) est délégué à NotificationPushService.
 *
 * Pour ALERTE_CREE, AlerteBroadcastService est appelé AVANT creerOutbox dans le listener
 * (broadcast immédiat) — les deux appels sont distincts et explicites dans handleNotification.
 */
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchServiceImpl.class);
    private static final List<Canal> CANAUX_ACTIFS = List.of(Canal.EMAIL, Canal.IN_APP);

    private final AlerteRepository alerteRepository;
    private final MesureRepository mesureRepository;
    private final SuperviseurRepository superviseurRepository;
    private final AdminRepository adminRepository;
    private final NotificationRepository notificationRepository;
    private final EnvoiNotificationRepository envoiNotificationRepository;
    private final NotificationPushService notificationPushService;
    private final AlerteBroadcastService alerteBroadcastService;

    public NotificationDispatchServiceImpl(
            AlerteRepository alerteRepository,
            MesureRepository mesureRepository,
            SuperviseurRepository superviseurRepository,
            AdminRepository adminRepository,
            NotificationRepository notificationRepository,
            EnvoiNotificationRepository envoiNotificationRepository,
            NotificationPushService notificationPushService,
            AlerteBroadcastService alerteBroadcastService
    ) {
        this.alerteRepository = alerteRepository;
        this.mesureRepository = mesureRepository;
        this.superviseurRepository = superviseurRepository;
        this.adminRepository = adminRepository;
        this.notificationRepository = notificationRepository;
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.notificationPushService = notificationPushService;
        this.alerteBroadcastService = alerteBroadcastService;
    }

    // ── Points d'entrée ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void dispatcherAlerte(UUID idAlerte) {
        alerteRepository.findById(idAlerte).ifPresentOrElse(alerte -> {
            DonneesAlerte donnees = resoudreDonneesAlerte(alerte);

            // Broadcast global (dashboard temps réel) — données déjà résolues, pas de 2ème chargement
            alerteBroadcastService.publierAlerteEtKpis(
                    alerte, "CREATION",
                    donnees.idPointMesureLong(),
                    donnees.nomPointMesure()
            );

            // Notifications personnelles (EMAIL outbox + IN_APP push)
            creerOutbox(alerte, TypeEvenement.ALERTE_CREE,
                    construireTitreAlerteCree(alerte, donnees.nomPointMesure()),
                    donnees.versMap(TypeEvenement.ALERTE_CREE));

        }, () -> logger.warn("Alerte {} introuvable — dispatch ignoré", idAlerte));
    }

    @Override
    @Transactional
    public void dispatcherAlerteResolue(UUID idAlerte) {
        alerteRepository.findById(idAlerte).ifPresentOrElse(alerte -> {
            DonneesAlerte donnees = resoudreDonneesAlerte(alerte);

            // Broadcast global
            alerteBroadcastService.publierAlerteEtKpis(
                    alerte, "RESOLUTION", null, null);

            // Notifications personnelles
            creerOutbox(alerte, TypeEvenement.ALERTE_RESOLU,
                    construireTitreAlerteResolu(alerte, donnees.nomPointMesure()),
                    donnees.versMap(TypeEvenement.ALERTE_RESOLU));

        }, () -> logger.warn("Alerte {} introuvable pour dispatch résolution", idAlerte));
    }

    @Override
    public void dispatcherCompteActive(Superviseur superviseur) {
        String titre = String.format("Nouveau compte activé - %s %s",
                nvl(superviseur.getPrenom()), nvl(superviseur.getNom())).strip();
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("idSuperviseur", superviseur.getIdSuperviseur().toString());
        donnees.put("nomSuperviseur", nvl(superviseur.getNom()));
        donnees.put("prenomSuperviseur", nvl(superviseur.getPrenom()));
        donnees.put("dateActivation", LocalDateTime.now().toString());
        creerOutbox(null, TypeEvenement.COMPTE_ACTIVEE, titre, donnees);
    }

    @Override
    public void dispatcherSeuilModifie(String nomPointMesure, Metrique metrique, boolean estAbsolu) {
        String typeLabel = estAbsolu ? "Seuil absolu modifié" : "Marge dynamique modifiée";
        String titre = String.format("%s - %s (%s)", typeLabel, nomPointMesure, labelMetrique(metrique));
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomPointMesure", nomPointMesure);
        donnees.put("metrique", metrique.name());
        donnees.put("typeModification", estAbsolu ? "SEUIL_ABSOLU" : "MARGE_DYNAMIQUE");
        donnees.put("dateModification", LocalDateTime.now().toString());
        creerOutbox(null, TypeEvenement.CONFIG_SEUILS_MODIFIE, titre, donnees);
    }

    @Override
    public void dispatcherRapportGenere(String nomRapport) {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("idRapport", nomRapport != null ? nomRapport : "");
        donnees.put("dateGeneration", LocalDateTime.now().toString());
        creerOutbox(null, TypeEvenement.RAPPORT_GENERE, "Rapport disponible", donnees);
    }

    // ── Résolution des données brutes ─────────────────────────────────────────

    private DonneesAlerte resoudreDonneesAlerte(Alerte alerte) {
        String nomPointMesure = "inconnu";
        String typeEmplacement = "CABINE";
        Long idPointMesureLong = null;

        Mesure mesure = mesureRepository.findByIdWithPointMesure(alerte.getIdMesure());
        if (mesure != null && mesure.getPointMesure() != null) {
            nomPointMesure = mesure.getPointMesure().getNom();
            idPointMesureLong = mesure.getPointMesure().getId();
            typeEmplacement = mesure.getPointMesure().getTypeEmplacement() != null
                    ? mesure.getPointMesure().getTypeEmplacement() : "CABINE";
        }
        return new DonneesAlerte(
                alerte.getIdAlerte().toString(),
                alerte.getMetrique().name(),
                alerte.getTypeAlerte().name(),
                alerte.getSeverite().name(),
                nomPointMesure,
                idPointMesureLong,
                typeEmplacement,
                alerte.getCreatedAt() != null ? alerte.getCreatedAt().toString() : "",
                alerte.getUpdatedAt() != null ? alerte.getUpdatedAt().toString() : ""
        );
    }

    /**
     * Données brutes résolues une seule fois — partagées par AlerteBroadcastService
     * (broadcast dashboard) et creerOutbox (notifications personnelles).
     * Élimine le double chargement PointMesure identifié à l'audit.
     */
    private record DonneesAlerte(
            String idAlerte, String metrique, String typeAlerte, String severite,
            String nomPointMesure, Long idPointMesureLong, String typeEmplacement,
            String dateCreation, String dateResolution
    ) {
        Map<String, Object> versMap(TypeEvenement type) {
            Map<String, Object> m = new HashMap<>();
            m.put("idAlerte", idAlerte);
            m.put("metrique", metrique);
            m.put("typeAlerte", typeAlerte);
            m.put("severite", severite);
            m.put("nomPointMesure", nomPointMesure);
            m.put("typeEmplacement", typeEmplacement);
            m.put("dateCreation", dateCreation);
            m.put("dateEvenement", type == TypeEvenement.ALERTE_RESOLU ? dateResolution : dateCreation);
            return m;
        }
    }

    // ── Titres courts ─────────────────────────────────────────────────────────

    private String construireTitreAlerteCree(Alerte alerte, String nomPointMesure) {
        String sev = alerte.getSeverite() == Severite.CRITIQUE ? "critique" : "moyenne";
        String type = alerte.getTypeAlerte() == TypeAlerte.SEUIL_ABSOLU
                ? "Seuil absolu dépassé" : "Anomalie dynamique détectée";
        return String.format("Alerte %s - %s (%s, %s)", sev, type,
                labelMetrique(alerte.getMetrique()), nomPointMesure);
    }

    private String construireTitreAlerteResolu(Alerte alerte, String nomPointMesure) {
        return String.format("Alerte résolue - %s, %s",
                labelMetrique(alerte.getMetrique()), nomPointMesure);
    }

    // ── Outbox (privée — appelée uniquement par les méthodes dispatcher*) ─────

    /**
     * Orchestre la persistance et le push IN_APP personnel.
     *
     * Privée : non exposée dans NotificationDispatchService (interface),
     * appelée uniquement par les 5 méthodes dispatcher* de cette classe.
     */
    private void creerOutbox(Object entiteSource, TypeEvenement typeEvenement,
                             String titre, Map<String, Object> donneesEvenement) {
        Notification notification = new Notification();
        if (entiteSource instanceof Alerte alerte) notification.setAlerte(alerte);
        notification.setTypeEvenement(typeEvenement);
        notification.setTitre(titre);
        notification.setDonneesEvenement(donneesEvenement);
        Notification saved = notificationRepository.save(notification);

        List<UUID> destinataireIds = destinatairesPourEvenement(typeEvenement);
        if (destinataireIds.isEmpty()) {
            logger.info("[OUTBOX] {} — aucun destinataire actif", typeEvenement);
            return;
        }

        int nbCrees = 0;
        for (UUID idDestinataire : destinataireIds) {
            for (Canal canal : CANAUX_ACTIFS) {
                if (envoiNotificationRepository
                        .existsByNotification_IdNotificationAndSuperviseur_IdSuperviseurAndCanal(
                                saved.getIdNotification(), idDestinataire, canal)) continue;

                if (canal == Canal.EMAIL && !isEmailValide(resolveEmail(idDestinataire))) {
                    logger.warn("[OUTBOX] Email invalide pour {} — ligne EMAIL non créée", idDestinataire);
                    continue;
                }

                Superviseur superviseur = superviseurRepository.findById(idDestinataire).orElse(null);
                if (superviseur == null) {
                    logger.warn("[OUTBOX] Superviseur {} introuvable", idDestinataire);
                    continue;
                }

                EnvoiNotification envoi = new EnvoiNotification();
                envoi.setSuperviseur(superviseur);
                envoi.setNotification(saved);
                envoi.setCanal(canal);

                if (canal == Canal.IN_APP) {
                    envoi.setStatutEnvoi(StatutEnvoi.ENVOYE);
                    envoi.setDateEnvoi(LocalDateTime.now());
                    envoiNotificationRepository.save(envoi);
                    notificationPushService.pousser(
                            envoi.getIdEnvoi(), saved.getIdNotification(),
                            idDestinataire, typeEvenement, titre,
                            donneesEvenement, saved.getCreatedAt());
                } else {
                    envoi.setStatutEnvoi(StatutEnvoi.EN_ATTENTE);
                    envoiNotificationRepository.save(envoi);
                }
                nbCrees++;
            }
        }

        logger.info("[OUTBOX] {} — {} EnvoiNotification créés (notification={})",
                typeEvenement, nbCrees, saved.getIdNotification());
    }

    // ── Mapping destinataires ─────────────────────────────────────────────────

    private List<UUID> destinatairesPourEvenement(TypeEvenement typeEvenement) {
        return switch (typeEvenement) {
            case ALERTE_CREE, ALERTE_RESOLU, CONFIG_SEUILS_MODIFIE, RAPPORT_GENERE ->
                superviseurRepository.findAll().stream()
                        .filter(s -> s.isActif() && s.isCompteActive() && s.getDeletedAt() == null)
                        .map(Superviseur::getIdSuperviseur).toList();
            case COMPTE_ACTIVEE ->
                adminRepository.findAll().stream()
                        .map(Admin::getSuperviseur)
                        .filter(s -> s != null && s.isActif()
                                && s.isCompteActive() && s.getDeletedAt() == null)
                        .map(Superviseur::getIdSuperviseur).toList();
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveEmail(UUID id) {
        return superviseurRepository.findById(id).map(Superviseur::getEmail).orElse(null);
    }

    private boolean isEmailValide(String email) {
        if (email == null || email.isBlank()) return false;
        int at = email.indexOf('@');
        if (at < 1) return false;
        String dom = email.substring(at + 1);
        return dom.contains(".") && dom.length() >= 3;
    }

    private String labelMetrique(Metrique metrique) {
        return metrique == Metrique.TEMPERATURE ? "Température" : "Humidité";
    }

    private String nvl(String val) { return val != null ? val : ""; }
}
