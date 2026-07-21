package com.projet.notifications.model;

import com.projet.alerting.model.Alerte;
import com.projet.notifications.model.enums.TypeEvenement;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: notifications
 * SQL Table: notification
 *
 * Written and read by Java service. One notification is created per event 
 * (one per unique event, not per recipient). See EnvoiNotification for delivery tracking.
 * The nullable FK to alerte is SET NULL on delete (per migration constraint).
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_notification")
    private UUID idNotification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerte")  // nullable: ON DELETE SET NULL
    private Alerte alerte;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 30)
    private TypeEvenement typeEvenement;

    @Column(length = 255)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Notification() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdNotification() { return idNotification; }
    public void setIdNotification(UUID idNotification) { this.idNotification = idNotification; }

    public Alerte getAlerte() { return alerte; }
    public void setAlerte(Alerte alerte) { this.alerte = alerte; }

    public TypeEvenement getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(TypeEvenement typeEvenement) { this.typeEvenement = typeEvenement; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
