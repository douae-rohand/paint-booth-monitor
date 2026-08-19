package com.projet.notifications.model;

import com.projet.alerting.model.Alerte;
import com.projet.notifications.model.enums.TypeEvenement;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Module: notifications
 * SQL Table: notification
 *
 * Une ligne par événement métier (pas par destinataire ni par canal).
 * Voir EnvoiNotification pour le suivi de livraison par destinataire × canal.
 *
 * donnees_evenement (JSONB) stocke les données brutes structurées de l'événement.
 * Chaque canal (EMAIL, IN_APP) produit sa propre mise en forme à partir de ces données.
 * Structures par TypeEvenement :
 *
 *   ALERTE_CREE / ALERTE_RESOLU :
 *     { idAlerte, metrique, typeAlerte, severite, nomPointMesure, dateEvenement }
 *
 *   COMPTE_ACTIVEE :
 *     { idSuperviseur, nomSuperviseur, prenomSuperviseur, dateActivation }
 *
 *   CONFIG_SEUILS_MODIFIE :
 *     { idPointMesure, nomPointMesure, metrique, typeModification, dateModification }
 *
 *   RAPPORT_GENERE :
 *     { idRapport, dateGeneration }
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

    /**
     * Données brutes structurées de l'événement (JSONB).
     * Source unique partagée par tous les canaux — chacun formate selon ses besoins.
     * Ne jamais stocker de texte pré-formaté ici.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_evenement", columnDefinition = "jsonb")
    private Map<String, Object> donneesEvenement;

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

    public Map<String, Object> getDonneesEvenement() { return donneesEvenement; }
    public void setDonneesEvenement(Map<String, Object> donneesEvenement) { this.donneesEvenement = donneesEvenement; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
