package com.projet.notifications.model;

import com.projet.auth.model.Superviseur;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.StatutEnvoi;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: notifications
 * SQL Table: envoi_notification
 *
 * Written and read by Java service. One row per (notification, superviseur, canal) triplet.
 * Tracks delivery status and read status per recipient.
 */
@Entity
@Table(name = "envoi_notification")
public class EnvoiNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_envoi")
    private UUID idEnvoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_notification", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_envoi", nullable = false, length = 15)
    private StatutEnvoi statutEnvoi = StatutEnvoi.EN_ATTENTE;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public EnvoiNotification() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdEnvoi() { return idEnvoi; }
    public void setIdEnvoi(UUID idEnvoi) { this.idEnvoi = idEnvoi; }

    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }

    public Canal getCanal() { return canal; }
    public void setCanal(Canal canal) { this.canal = canal; }

    public StatutEnvoi getStatutEnvoi() { return statutEnvoi; }
    public void setStatutEnvoi(StatutEnvoi statutEnvoi) { this.statutEnvoi = statutEnvoi; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
