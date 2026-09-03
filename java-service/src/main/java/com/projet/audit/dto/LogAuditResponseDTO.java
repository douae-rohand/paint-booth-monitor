package com.projet.audit.dto;

import com.projet.audit.model.LogAudit;
import com.projet.audit.model.enums.ActionAudit;

import java.time.LocalDateTime;
import java.util.UUID;

public class LogAuditResponseDTO {

    private UUID idLog;
    private UUID idSuperviseur;
    private String nomSuperviseur;
    private String prenomSuperviseur;
    private String emailSuperviseur;
    private ActionAudit action;
    private LocalDateTime dateAction;

    public LogAuditResponseDTO() {}

    public LogAuditResponseDTO(UUID idLog, UUID idSuperviseur, String nomSuperviseur, String prenomSuperviseur, String emailSuperviseur, ActionAudit action, LocalDateTime dateAction) {
        this.idLog = idLog;
        this.idSuperviseur = idSuperviseur;
        this.nomSuperviseur = nomSuperviseur;
        this.prenomSuperviseur = prenomSuperviseur;
        this.emailSuperviseur = emailSuperviseur;
        this.action = action;
        this.dateAction = dateAction;
    }

    public static LogAuditResponseDTO from(LogAudit log) {
        if (log == null) return null;
        UUID idSup = log.getSuperviseur() != null ? log.getSuperviseur().getIdSuperviseur() : null;
        String nom = log.getSuperviseur() != null ? log.getSuperviseur().getNom() : null;
        String prenom = log.getSuperviseur() != null ? log.getSuperviseur().getPrenom() : null;
        String email = log.getSuperviseur() != null ? log.getSuperviseur().getEmail() : null;

        return new LogAuditResponseDTO(
                log.getIdLog(),
                idSup,
                nom,
                prenom,
                email,
                log.getAction(),
                log.getDateAction()
        );
    }

    public UUID getIdLog() { return idLog; }
    public void setIdLog(UUID idLog) { this.idLog = idLog; }

    public UUID getIdSuperviseur() { return idSuperviseur; }
    public void setIdSuperviseur(UUID idSuperviseur) { this.idSuperviseur = idSuperviseur; }

    public String getNomSuperviseur() { return nomSuperviseur; }
    public void setNomSuperviseur(String nomSuperviseur) { this.nomSuperviseur = nomSuperviseur; }

    public String getPrenomSuperviseur() { return prenomSuperviseur; }
    public void setPrenomSuperviseur(String prenomSuperviseur) { this.prenomSuperviseur = prenomSuperviseur; }

    public String getEmailSuperviseur() { return emailSuperviseur; }
    public void setEmailSuperviseur(String emailSuperviseur) { this.emailSuperviseur = emailSuperviseur; }

    public ActionAudit getAction() { return action; }
    public void setAction(ActionAudit action) { this.action = action; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
}
