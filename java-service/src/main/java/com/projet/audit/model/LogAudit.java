package com.projet.audit.model;

import com.projet.audit.model.enums.ActionAudit;
import com.projet.auth.model.Superviseur;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: audit
 * SQL Table: log_audit
 * 
 * Audit log mapping to record user actions.
 * 
 * Note: Table is INSERT-only (write-only). No update or delete operations
 * should ever be called on this entity from Java. Setter methods are omitted
 * except for setup during construction.
 */
@Entity
@Table(name = "log_audit")
public class LogAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_log", updatable = false)
    private UUID idLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false, updatable = false)
    private Superviseur superviseur;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private ActionAudit action;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────────────────────

    public LogAudit() {}

    public LogAudit(Superviseur superviseur, ActionAudit action) {
        this.superviseur = superviseur;
        this.action = action;
    }

    // ── Accessors (Getters Only to Enforce Immuntability) ─────────────────────

    public UUID getIdLog() { return idLog; }

    public Superviseur getSuperviseur() { return superviseur; }

    public ActionAudit getAction() { return action; }

    public LocalDateTime getDateAction() { return dateAction; }
}
