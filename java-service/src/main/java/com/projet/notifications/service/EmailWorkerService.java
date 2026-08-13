package com.projet.notifications.service;

import com.projet.alerting.model.Alerte;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.StatutEnvoi;
import com.projet.notifications.repository.EnvoiNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Worker de traitement de la file d'envoi email (pattern outbox).
 *
 * Tourne dans le thread du scheduler Spring (@Scheduled), complètement découplé
 * du thread d'écoute LISTEN PostgreSQL. Le fournisseur d'email n'est jamais appelé
 * depuis ce dernier.
 *
 * Ce service dépend uniquement de l'interface {@link EmailService} — il ne connaît
 * pas SendGrid. Pour changer de fournisseur, aucune modification n'est nécessaire ici.
 *
 * Logique de retry basée sur {@link EmailService.EmailResult#statut()} :
 *   SUCCES           → statut_envoi = ENVOYE
 *   ECHEC_TEMPORAIRE → reste EN_ATTENTE (retry au prochain cycle, tentatives non incrémentées
 *                      pour les rate limits ; incrémentées pour les autres erreurs transitoires)
 *   ECHEC_DEFINITIF  → statut_envoi = ECHEC immédiatement, sans attendre maxTentatives
 *
 * Configuration via application.yml :
 *   email.worker.batch-size       (défaut : 20)
 *   email.worker.max-tentatives   (défaut : 5)
 *   email.worker.cron             (défaut : toutes les 5 secondes)
 */
@Service
public class EmailWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(EmailWorkerService.class);

    private final EnvoiNotificationRepository envoiNotificationRepository;
    private final EmailService emailService;
    private final MesureRepository mesureRepository;

    @Value("${email.worker.batch-size:20}")
    private int batchSize;

    @Value("${email.worker.max-tentatives:5}")
    private int maxTentatives;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSSSSS");

    public EmailWorkerService(
            EnvoiNotificationRepository envoiNotificationRepository,
            EmailService emailService,
            MesureRepository mesureRepository
    ) {
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.emailService = emailService;
        this.mesureRepository = mesureRepository;
    }

    /**
     * Cycle principal du worker.
     * Cadence configurable via email.worker.cron.
     */
    @Scheduled(cron = "${email.worker.cron:*/5 * * * * *}")
    @Transactional
    public void traiterBatch() {
        List<EnvoiNotification> batch = envoiNotificationRepository.findBatchPourTraitement(
                Canal.EMAIL,
                StatutEnvoi.EN_ATTENTE,
                PageRequest.of(0, batchSize)
        );

        if (batch.isEmpty()) {
            return;
        }

        logger.info("[EMAIL WORKER] Traitement de {} envoi(s) en attente", batch.size());

        for (EnvoiNotification envoi : batch) {
            traiterEnvoi(envoi);
        }
    }

    // ── Traitement d'un envoi ─────────────────────────────────────────────────

    private void traiterEnvoi(EnvoiNotification envoi) {
        String emailDestinataire = envoi.getSuperviseur().getEmail();
        String sujet = envoi.getNotification().getTitre();

        EmailService.EmailResult result = construireEtEnvoyer(envoi, emailDestinataire, sujet);

        switch (result.statut()) {

            case SUCCES -> {
                marquerEnvoye(envoi);
                logger.info("[EMAIL WORKER] id_envoi={} → ENVOYE", envoi.getIdEnvoi());
            }

            case ECHEC_TEMPORAIRE -> {
                // Erreur transitoire (réseau, rate limit, 5xx) : incrémenter tentatives
                // et rester EN_ATTENTE pour retry, sauf si maxTentatives atteint.
                int nouvellesTentatives = envoi.getTentatives() + 1;
                envoi.setTentatives(nouvellesTentatives);
                envoi.setDerniereErreur(result.erreur());
                envoi.setUpdatedAt(LocalDateTime.now());

                if (nouvellesTentatives >= maxTentatives) {
                    marquerEchecDefinitif(envoi, result.erreur());
                    logger.error("[EMAIL WORKER] id_envoi={} → ECHEC définitif après {} tentative(s) (temporaire épuisé)",
                            envoi.getIdEnvoi(), nouvellesTentatives);
                } else {
                    envoiNotificationRepository.save(envoi);
                    logger.warn("[EMAIL WORKER] id_envoi={} → tentative {}/{} - ECHEC_TEMPORAIRE, retry programmé",
                            envoi.getIdEnvoi(), nouvellesTentatives, maxTentatives);
                }
            }

            case ECHEC_DEFINITIF -> {
                // Erreur permanente (auth, config, adresse invalide) : ECHEC immédiat,
                // un retry ne servirait à rien sans intervention manuelle.
                marquerEchecDefinitif(envoi, result.erreur());
                logger.error("[EMAIL WORKER] id_envoi={} → ECHEC définitif immédiat (ECHEC_DEFINITIF)",
                        envoi.getIdEnvoi());
            }
        }
    }

    // ── Construction et envoi ──────────────────────────────────────────────────

    /**
     * Choisit la méthode d'envoi adaptée :
     *  - Si la notification est liée à une Alerte → email HTML riche (template commun + bouton dashboard)
     *  - Sinon (alerte supprimée ou autre type d'événement) → texte brut de secours
     */
    private EmailService.EmailResult construireEtEnvoyer(EnvoiNotification envoi, String emailDestinataire, String sujet) {
        Alerte alerte = envoi.getNotification().getAlerte();

        if (alerte != null) {
            String dateHeure = alerte.getCreatedAt() != null
                    ? alerte.getCreatedAt().format(DATE_FORMATTER)
                    : "-";
            String urlDashboard = frontendUrl + "/alertes";

            String emplacement = "l'équipement";
            String pointMesureNom = "Inconnu";

            try {
                Mesure mesure = mesureRepository.findByIdWithPointMesure(alerte.getIdMesure());
                if (mesure != null && mesure.getPointMesure() != null) {
                    PointMesure pm = mesure.getPointMesure();
                    emplacement = pm.getTypeEmplacement();
                    pointMesureNom = pm.getNom();
                }
            } catch (Exception e) {
                logger.error("[EMAIL WORKER] Impossible de charger le point de mesure pour l'alerte {}", alerte.getIdAlerte(), e);
            }

            return emailService.envoyerNotificationAlerte(
                    emailDestinataire,
                    sujet,
                    alerte.getMetrique().name(),
                    alerte.getTypeAlerte().name(),
                    alerte.getSeverite().name(),
                    dateHeure,
                    alerte.getIdAlerte().toString(),
                    urlDashboard,
                    emplacement,
                    pointMesureNom
            );
        }

        // Fallback texte brut si l'alerte a été supprimée (ON DELETE SET NULL)
        logger.warn("[EMAIL WORKER] id_envoi={} - alerte introuvable (SET NULL), envoi texte brut", envoi.getIdEnvoi());
        return emailService.envoyerNotification(
                emailDestinataire,
                sujet,
                envoi.getNotification().getContenu()
        );
    }

    // ── Helpers de transition d'état ──────────────────────────────────────────


    private void marquerEnvoye(EnvoiNotification envoi) {
        envoi.setStatutEnvoi(StatutEnvoi.ENVOYE);
        envoi.setDateEnvoi(LocalDateTime.now());
        envoi.setUpdatedAt(LocalDateTime.now());
        envoiNotificationRepository.save(envoi);
    }

    private void marquerEchecDefinitif(EnvoiNotification envoi, String messageErreur) {
        envoi.setStatutEnvoi(StatutEnvoi.ECHEC);
        envoi.setDerniereErreur(messageErreur);
        envoi.setUpdatedAt(LocalDateTime.now());
        envoiNotificationRepository.save(envoi);
    }
}
