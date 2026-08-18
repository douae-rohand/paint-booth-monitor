package com.projet.notifications.service;

import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.Notification;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.StatutEnvoi;
import com.projet.notifications.model.enums.TypeEvenement;
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
import java.util.Map;

/**
 * Worker de traitement de la file d'envoi email (pattern outbox).
 *
 * Responsabilité unique : lire les lignes EnvoiNotification(canal=EMAIL, statut=EN_ATTENTE),
 * dispatcher vers la bonne méthode EmailService selon typeEvenement, gérer retry/statut.
 *
 * Toutes les données nécessaires sont dans Notification.donnees_evenement (JSONB).
 * Le titre est lu depuis Notification.titre — jamais reconstruit ici.
 * Aucun accès DB à Alerte, PointMesure ou autre entité métier.
 *
 * Dispatch par TypeEvenement :
 *   ALERTE_CREE / ALERTE_RESOLU → envoyerNotificationAlerte     (template HTML riche)
 *   COMPTE_ACTIVEE              → envoyerNotificationCompteActive  (template HTML)
 *   CONFIG_SEUILS_MODIFIE       → envoyerNotificationSeuilModifie  (template HTML)
 *   RAPPORT_GENERE              → envoyerNotificationRapportGenere (template HTML)
 *   fallback (données null)     → envoyerNotification              (texte brut, sécurité)
 */
@Service
public class EmailWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(EmailWorkerService.class);

    private final EnvoiNotificationRepository envoiNotificationRepository;
    private final EmailService emailService;

    @Value("${email.worker.batch-size:20}")
    private int batchSize;

    @Value("${email.worker.max-tentatives:5}")
    private int maxTentatives;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public EmailWorkerService(
            EnvoiNotificationRepository envoiNotificationRepository,
            EmailService emailService
    ) {
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.emailService = emailService;
    }

    // ── Cycle principal ───────────────────────────────────────────────────────

    @Scheduled(cron = "${email.worker.cron:*/5 * * * * *}")
    @Transactional
    public void traiterBatch() {
        List<EnvoiNotification> batch = envoiNotificationRepository.findBatchPourTraitement(
                Canal.EMAIL, StatutEnvoi.EN_ATTENTE, PageRequest.of(0, batchSize));
        if (batch.isEmpty()) return;
        logger.info("[EMAIL WORKER] Traitement de {} envoi(s) en attente", batch.size());
        for (EnvoiNotification envoi : batch) traiterEnvoi(envoi);
    }

    // ── Traitement + retry ────────────────────────────────────────────────────

    private void traiterEnvoi(EnvoiNotification envoi) {
        String emailDestinataire = envoi.getSuperviseur().getEmail();
        EmailService.EmailResult result = dispatcher(envoi, emailDestinataire);

        switch (result.statut()) {
            case SUCCES -> {
                marquerEnvoye(envoi);
                logger.info("[EMAIL WORKER] id_envoi={} → ENVOYE", envoi.getIdEnvoi());
            }
            case ECHEC_TEMPORAIRE -> {
                int tentatives = envoi.getTentatives() + 1;
                envoi.setTentatives(tentatives);
                envoi.setDerniereErreur(result.erreur());
                envoi.setUpdatedAt(LocalDateTime.now());
                if (tentatives >= maxTentatives) {
                    marquerEchecDefinitif(envoi, result.erreur());
                    logger.error("[EMAIL WORKER] id_envoi={} → ECHEC définitif après {} tentative(s)",
                            envoi.getIdEnvoi(), tentatives);
                } else {
                    envoiNotificationRepository.save(envoi);
                    logger.warn("[EMAIL WORKER] id_envoi={} → tentative {}/{} - retry programmé",
                            envoi.getIdEnvoi(), tentatives, maxTentatives);
                }
            }
            case ECHEC_DEFINITIF -> {
                marquerEchecDefinitif(envoi, result.erreur());
                logger.error("[EMAIL WORKER] id_envoi={} → ECHEC définitif immédiat", envoi.getIdEnvoi());
            }
        }
    }

    // ── Dispatch par TypeEvenement ────────────────────────────────────────────

    private EmailService.EmailResult dispatcher(EnvoiNotification envoi, String email) {
        Notification n   = envoi.getNotification();
        TypeEvenement type = n.getTypeEvenement();
        Map<String, Object> d = n.getDonneesEvenement();
        String titre = nvl(n.getTitre()); // lu depuis Notification.titre — jamais reconstruit

        if (type == null || d == null) {
            logger.warn("[EMAIL WORKER] id_envoi={} - données manquantes, fallback texte brut",
                    envoi.getIdEnvoi());
            return emailService.envoyerNotification(email, titre, titre);
        }

        return switch (type) {
            case ALERTE_CREE, ALERTE_RESOLU -> emailService.envoyerNotificationAlerte(email, titre, d);
            case COMPTE_ACTIVEE       -> emailService.envoyerNotificationCompteActive(email, titre, d);
            case CONFIG_SEUILS_MODIFIE -> emailService.envoyerNotificationSeuilModifie(email, titre, d);
            case RAPPORT_GENERE       -> emailService.envoyerNotificationRapportGenere(email, titre, d);
        };
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

    private String nvl(Map<String, Object> d, String cle) {
        Object val = d.get(cle);
        return val != null ? val.toString() : "";
    }

    private String nvl(String val) { return val != null ? val : ""; }
}
