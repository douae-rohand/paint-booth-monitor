package com.projet.notifications.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.gateway.dto.AlerteMessage;
import com.projet.gateway.dto.KpiMessage;
import com.projet.kpis.service.KpiService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service de dispatch des alertes.
 *
 * Deux responsabilités distinctes :
 *  1. Publication WebSocket immédiate (dashboard temps réel) — conservée du stub précédent.
 *  2. Création de la Notification + des EnvoiNotification EN_ATTENTE dans la base (outbox),
 *     pour traitement asynchrone par EmailWorkerService via @Scheduled.
 *
 * Cette méthode est appelée depuis le thread d'écoute LISTEN PostgreSQL.
 * Elle ne doit JAMAIS appeler SendGrid directement — uniquement des écritures DB rapides.
 */
@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchServiceImpl.class);

    private final AlerteRepository alerteRepository;
    private final KpiService kpiService;
    private final MesureRepository mesureRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SuperviseurRepository superviseurRepository;
    private final NotificationRepository notificationRepository;
    private final EnvoiNotificationRepository envoiNotificationRepository;

    public NotificationDispatchServiceImpl(
            AlerteRepository alerteRepository,
            KpiService kpiService,
            MesureRepository mesureRepository,
            SimpMessagingTemplate messagingTemplate,
            SuperviseurRepository superviseurRepository,
            NotificationRepository notificationRepository,
            EnvoiNotificationRepository envoiNotificationRepository
    ) {
        this.alerteRepository = alerteRepository;
        this.kpiService = kpiService;
        this.mesureRepository = mesureRepository;
        this.messagingTemplate = messagingTemplate;
        this.superviseurRepository = superviseurRepository;
        this.notificationRepository = notificationRepository;
        this.envoiNotificationRepository = envoiNotificationRepository;
    }

    /**
     * Point d'entrée appelé par PostgresNotificationListener sur chaque NOTIFY nouvelle_alerte.
     *
     * Flux :
     *   1. Charge l'alerte depuis la DB.
     *   2. Publie sur /topic/alertes et /topic/kpis (WebSocket — temps réel dashboard).
     *   3. Crée 1 Notification (outbox) + N EnvoiNotification EN_ATTENTE (un par superviseur actif).
     *
     * Tout est exécuté dans une seule transaction (@Transactional) : si l'écriture outbox échoue,
     * rien n'est persisté — le prochain NOTIFY PostgreSQL (ou retry du listener) retentera.
     */
    @Override
    @Transactional
    public void dispatcherAlerte(UUID idAlerte) {
        alerteRepository.findById(idAlerte).ifPresentOrElse(alerte -> {
            // 1. Publication WebSocket (ne bloque pas, non transactionnelle)
            publishAlerteWebSocket(alerte);
            publishKpisWebSocket();

            // 2. Écriture outbox (dans la même transaction)
            creerOutboxAlerte(alerte);

        }, () -> logger.warn("Alerte {} introuvable — dispatch ignoré", idAlerte));
    }

    // ── Outbox ───────────────────────────────────────────────────────────────

    /**
     * Crée 1 Notification + les EnvoiNotification EMAIL EN_ATTENTE correspondants.
     *
     * Destinataires : tous les superviseurs actifs ayant un compte activé.
     * (Quand ConfigurationDestinataire sera utilisée, remplacer la requête par une
     *  jointure sur configuration_destinataire WHERE type_evenement='ALERTE' AND canal='EMAIL' AND actif=true.)
     *
     * Idempotence : existsByNotification...AndCanal empêche un double insert si le même
     * NOTIFY est reçu deux fois (reconnexion PLC ou NOTIFY dupliqué).
     */
    private void creerOutboxAlerte(Alerte alerte) {
        // Construire le message
        String titre = construireTitre(alerte);
        String contenu = construireContenu(alerte);

        // 1 Notification par événement (pas par destinataire)
        Notification notification = new Notification();
        notification.setAlerte(alerte);
        notification.setTypeEvenement(TypeEvenement.ALERTE);
        notification.setTitre(titre);
        notification.setContenu(contenu);
        Notification savedNotification = notificationRepository.save(notification);

        // Destinataires : superviseurs actifs avec compte activé
        // TODO: filtrer via ConfigurationDestinataire quand la config UI est disponible
        List<Superviseur> destinataires = superviseurRepository
                .findAll()
                .stream()
                .filter(s -> s.isActif() && s.isCompteActive() && s.getDeletedAt() == null)
                .toList();

        if (destinataires.isEmpty()) {
            logger.info("[OUTBOX] Alerte {} — aucun superviseur actif, pas d'envoi créé", alerte.getIdAlerte());
            return;
        }

        int nbCrees = 0;
        for (Superviseur superviseur : destinataires) {
            // Idempotence : ne pas créer un doublon si le NOTIFY a déjà été traité
            boolean dejaPresent = envoiNotificationRepository
                    .existsByNotification_IdNotificationAndSuperviseur_IdSuperviseurAndCanal(
                            savedNotification.getIdNotification(),
                            superviseur.getIdSuperviseur(),
                            Canal.EMAIL
                    );
            if (dejaPresent) {
                logger.debug("[OUTBOX] EnvoiNotification déjà créé pour {} / alerte {} — skip",
                        superviseur.getIdSuperviseur(), alerte.getIdAlerte());
                continue;
            }

            // Valider l'email avant de créer la ligne (évite d'accumuler des ECHEC certains)
            if (!isEmailValide(superviseur.getEmail())) {
                logger.warn("[OUTBOX] Email invalide pour superviseur {} — EnvoiNotification non créé",
                        superviseur.getIdSuperviseur());
                continue;
            }

            EnvoiNotification envoi = new EnvoiNotification();
            envoi.setNotification(savedNotification);
            envoi.setSuperviseur(superviseur);
            envoi.setCanal(Canal.EMAIL);
            envoi.setStatutEnvoi(StatutEnvoi.EN_ATTENTE);
            envoiNotificationRepository.save(envoi);
            nbCrees++;
        }

        logger.info("[OUTBOX] Alerte {} — {} EnvoiNotification EMAIL EN_ATTENTE créés (notification={})",
                alerte.getIdAlerte(), nbCrees, savedNotification.getIdNotification());
    }

    // ── WebSocket ─────────────────────────────────────────────────────────────

    private void publishAlerteWebSocket(Alerte alerte) {
        var mesure = mesureRepository.findByIdWithPointMesure(alerte.getIdMesure());
        if (mesure == null) {
            logger.warn("Mesure {} non trouvée pour l'alerte {}", alerte.getIdMesure(), alerte.getIdAlerte());
            return;
        }
        var pointMesure = mesure.getPointMesure();
        if (pointMesure == null) {
            logger.warn("Point de mesure non trouvé pour la mesure {}", mesure.getIdMesure());
            return;
        }

        AlerteMessage message = new AlerteMessage(
                alerte.getIdAlerte(),
                pointMesure.getId(),
                pointMesure.getNom(),
                alerte.getMetrique(),
                alerte.getTypeAlerte(),
                alerte.getSeverite(),
                alerte.getCreatedAt()
        );
        messagingTemplate.convertAndSend("/topic/alertes", message);
        logger.info("[WS] Alerte {} publiée sur /topic/alertes", alerte.getIdAlerte());
    }

    private void publishKpisWebSocket() {
        var kpis = kpiService.getKpisGlobaux();
        KpiMessage message = new KpiMessage(kpis.getAlertesActives(), kpis.getNbPointsEnAnomalie());
        messagingTemplate.convertAndSend("/topic/kpis", message);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String construireTitre(Alerte alerte) {
        return String.format("[%s] Anomalie détectée — %s",
                alerte.getSeverite().name(),
                alerte.getMetrique().name());
    }

    private String construireContenu(Alerte alerte) {
        return String.format(
                "Une anomalie a été détectée sur la cabine de peinture.%n" +
                "Métrique     : %s%n" +
                "Type         : %s%n" +
                "Sévérité     : %s%n" +
                "Date/heure   : %s%n" +
                "ID alerte    : %s%n%n" +
                "Connectez-vous au tableau de bord pour consulter les détails.",
                alerte.getMetrique().name(),
                alerte.getTypeAlerte().name(),
                alerte.getSeverite().name(),
                alerte.getCreatedAt(),
                alerte.getIdAlerte()
        );
    }

    /**
     * Validation basique du format email.
     * Bloque les adresses manifestement malformées avant création de la ligne outbox.
     * Une validation plus stricte (DNS MX) serait disproportionnée ici.
     */
    private boolean isEmailValide(String email) {
        if (email == null || email.isBlank()) return false;
        // RFC 5321 simplifié : présence d'un '@' avec au moins 1 char de chaque côté et un point dans le domaine
        int atIndex = email.indexOf('@');
        if (atIndex < 1) return false;
        String domaine = email.substring(atIndex + 1);
        return domaine.contains(".") && domaine.length() >= 3;
    }
}
