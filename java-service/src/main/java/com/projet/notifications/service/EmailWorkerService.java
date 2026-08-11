package com.projet.notifications.service;

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
import java.util.List;

/**
 * Worker de traitement de la file d'envoi email (pattern outbox).
 *
 * Tourne dans le thread du scheduler Spring (@Scheduled), complètement découplé
 * du thread d'écoute LISTEN PostgreSQL. SendGrid n'est jamais appelé depuis ce dernier.
 *
 * Cycle de traitement :
 *  1. Sélectionne les lignes EnvoiNotification WHERE canal=EMAIL AND statut=EN_ATTENTE (batch limité).
 *  2. Pour chaque ligne, appelle SendGrid via SendGridEmailService.
 *  3. Succès → ENVOYE + date_envoi.
 *  4. Échec 429 → reste EN_ATTENTE (retry au prochain cycle, compteur non incrémenté).
 *  5. Échec 401/403 → ECHEC immédiat (retry inutile sans intervention manuelle).
 *  6. Autre échec → incrémente tentatives + enregistre l'erreur dans derniere_erreur.
 *     Si tentatives >= maxTentatives → ECHEC définitif.
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
    private final SendGridEmailService sendGridEmailService;

    @Value("${email.worker.batch-size:20}")
    private int batchSize;

    @Value("${email.worker.max-tentatives:5}")
    private int maxTentatives;

    public EmailWorkerService(
            EnvoiNotificationRepository envoiNotificationRepository,
            SendGridEmailService sendGridEmailService
    ) {
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.sendGridEmailService = sendGridEmailService;
    }

    /**
     * Cycle principal du worker.
     * Cadence configurable via email.worker.cron (défaut : toutes les 5 secondes).
     * fixedDelay serait une alternative simple, mais cron offre plus de flexibilité en config.
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
            return; // Rien à traiter — ne pas polluer les logs
        }

        logger.info("[EMAIL WORKER] Traitement de {} envoi(s) en attente", batch.size());

        for (EnvoiNotification envoi : batch) {
            traiterEnvoi(envoi);
        }
    }

    // ── Traitement d'un envoi ─────────────────────────────────────────────────

    private void traiterEnvoi(EnvoiNotification envoi) {
        String emailDestinataire = envoi.getSuperviseur().getEmail();
        String titre = envoi.getNotification().getTitre();
        String contenu = envoi.getNotification().getContenu();

        // Appel SDK SendGrid — jamais dans le thread LISTEN
        SendGridEmailService.SendGridResult result =
                sendGridEmailService.envoyerNotificationAlerte(emailDestinataire, titre, contenu);

        if (result.isSucces()) {
            marquerEnvoye(envoi);
            logger.info("[EMAIL WORKER] id_envoi={} → ENVOYE (code={})",
                    envoi.getIdEnvoi(), result.statusCode());

        } else if (result.isRateLimit()) {
            // 429 : ne pas incrémenter tentatives, laisser EN_ATTENTE pour retry
            logger.warn("[EMAIL WORKER] id_envoi={} → rate limit SendGrid (429), retry au prochain cycle",
                    envoi.getIdEnvoi());
            // Pas de modification de statut — le prochain @Scheduled reprendra cette ligne

        } else if (result.isEchecAuthConfiguration()) {
            // 401/403 : clé API invalide ou sender non vérifié → ECHEC immédiat
            marquerEchecDefinitif(envoi,
                    String.format("Erreur de configuration SendGrid (%d) — intervention manuelle requise : %s",
                            result.statusCode(), result.messageErreur()));
            logger.error("[EMAIL WORKER] id_envoi={} → ECHEC AUTH/CONFIG (code={}) — intervention requise",
                    envoi.getIdEnvoi(), result.statusCode());

        } else {
            // Autre erreur réseau ou 4xx/5xx
            int nouvellesTentatives = envoi.getTentatives() + 1;
            envoi.setTentatives(nouvellesTentatives);
            envoi.setDerniereErreur(result.messageErreur());
            envoi.setUpdatedAt(LocalDateTime.now());

            if (nouvellesTentatives >= maxTentatives) {
                marquerEchecDefinitif(envoi, result.messageErreur());
                logger.error("[EMAIL WORKER] id_envoi={} → ECHEC définitif après {} tentatives (code={})",
                        envoi.getIdEnvoi(), nouvellesTentatives, result.statusCode());
            } else {
                // Reste EN_ATTENTE pour retry au prochain cycle
                envoiNotificationRepository.save(envoi);
                logger.warn("[EMAIL WORKER] id_envoi={} → tentative {}/{} échouée (code={}), retry programmé",
                        envoi.getIdEnvoi(), nouvellesTentatives, maxTentatives, result.statusCode());
            }
        }
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
