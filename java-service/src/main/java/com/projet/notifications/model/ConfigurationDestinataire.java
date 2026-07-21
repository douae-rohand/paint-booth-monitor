package com.projet.notifications.model;

import com.projet.auth.model.Admin;
import com.projet.auth.model.Superviseur;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.Severite;
import com.projet.notifications.model.enums.TypeEvenement;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: notifications
 * SQL Table: configuration_destinataire
 *
 * Written and read by Java service. Defines which superviseur receives which 
 * events on which channel, as configured by an admin.
 */
@Entity
@Table(name = "configuration_destinataire")
public class ConfigurationDestinataire {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_configuration")
    private UUID idConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 30)
    private TypeEvenement typeEvenement;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Severite severite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_configuration", nullable = false, updatable = false)
    private LocalDateTime dateConfiguration = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ConfigurationDestinataire() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdConfiguration() { return idConfiguration; }
    public void setIdConfiguration(UUID idConfiguration) { this.idConfiguration = idConfiguration; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }

    public TypeEvenement getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(TypeEvenement typeEvenement) { this.typeEvenement = typeEvenement; }

    public Severite getSeverite() { return severite; }
    public void setSeverite(Severite severite) { this.severite = severite; }

    public Canal getCanal() { return canal; }
    public void setCanal(Canal canal) { this.canal = canal; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getDateConfiguration() { return dateConfiguration; }
    public void setDateConfiguration(LocalDateTime dateConfiguration) { this.dateConfiguration = dateConfiguration; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
