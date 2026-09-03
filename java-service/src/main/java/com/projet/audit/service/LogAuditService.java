package com.projet.audit.service;

import com.projet.audit.dto.LogAuditResponseDTO;
import com.projet.audit.model.LogAudit;
import com.projet.audit.model.enums.ActionAudit;
import com.projet.audit.repository.LogAuditRepository;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.SuperviseurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LogAuditService {

    private static final Logger logger = LoggerFactory.getLogger(LogAuditService.class);

    private final LogAuditRepository logAuditRepository;
    private final SuperviseurRepository superviseurRepository;

    public LogAuditService(LogAuditRepository logAuditRepository, SuperviseurRepository superviseurRepository) {
        this.logAuditRepository = logAuditRepository;
        this.superviseurRepository = superviseurRepository;
    }

    /**
     * Enregistre une action d'audit pour un superviseur donné.
     * En cas d'erreur lors de l'enregistrement, l'exception est capturée et loguée
     * via SLF4J afin de ne jamais faire échouer la transaction métier principale.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logger(UUID idSuperviseur, ActionAudit action) {
        if (idSuperviseur == null || action == null) {
            logger.warn("[AUDIT] Impossible d'enregistrer le log d'audit : idSuperviseur ou action nul (idSuperviseur={}, action={})", idSuperviseur, action);
            return;
        }

        try {
            Optional<Superviseur> superviseurOpt = superviseurRepository.findById(idSuperviseur);
            if (superviseurOpt.isEmpty()) {
                logger.warn("[AUDIT] Impossible d'enregistrer le log d'audit : superviseur non trouvé id={}", idSuperviseur);
                return;
            }

            LogAudit logAudit = new LogAudit(superviseurOpt.get(), action);
            logAuditRepository.save(logAudit);
            logger.debug("[AUDIT] Action enregistrée avec succès : superviseur={}, action={}", idSuperviseur, action);
        } catch (Exception e) {
            logger.error("[AUDIT] Échec de l'enregistrement du log d'audit pour superviseur={} action={} : {}",
                    idSuperviseur, action, e.getMessage(), e);
        }
    }

    /**
     * Recherche paginée et filtrée des logs d'audit.
     *
     * @param actions liste de valeurs ActionAudit (filtre IN, null ou vide = pas de filtre)
     */
    @Transactional(readOnly = true)
    public Page<LogAuditResponseDTO> listerLogs(
            UUID idSuperviseur,
            List<ActionAudit> actions,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable) {

        // Convertir la liste d'enum en String[] pour la requête native PostgreSQL
        // Si null ou vide → actionsEmpty=true → la clause IN est ignorée côté SQL
        boolean actionsEmpty = (actions == null || actions.isEmpty());
        String[] actionsArr = actionsEmpty
                ? new String[0]
                : actions.stream().map(ActionAudit::name).toArray(String[]::new);

        Page<LogAudit> page = logAuditRepository.findLogs(
                idSuperviseur, actionsArr, actionsEmpty, dateDebut, dateFin, pageable);
        return page.map(LogAuditResponseDTO::from);
    }
}
