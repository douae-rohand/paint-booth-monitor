package com.projet.audit.aspect;

import com.projet.audit.annotation.Audite;
import com.projet.audit.service.LogAuditService;
import com.projet.auth.model.Superviseur;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    private final LogAuditService logAuditService;

    public AuditAspect(LogAuditService logAuditService) {
        this.logAuditService = logAuditService;
    }

    @AfterReturning(pointcut = "@annotation(audite)", returning = "result")
    public void intercepterActionAuditee(Audite audite, Object result) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("[AUDIT ASPECT] Action {} interceptée mais aucun utilisateur authentifié dans SecurityContext", audite.value());
                return;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof Superviseur superviseur) {
                logAuditService.logger(superviseur.getIdSuperviseur(), audite.value());
            } else {
                logger.warn("[AUDIT ASPECT] Principal non reconnu (type={}) pour l'action {}", principal.getClass().getName(), audite.value());
            }
        } catch (Exception e) {
            logger.error("[AUDIT ASPECT] Erreur lors de l'interception AOP de l'audit pour {} : {}", audite.value(), e.getMessage(), e);
        }
    }
}
