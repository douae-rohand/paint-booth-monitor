package com.projet.notifications.repository;

import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.enums.Canal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository dédié aux requêtes IN_APP pour le bell icon.
 */
public interface NotificationInAppRepository extends JpaRepository<EnvoiNotification, UUID> {

    /**
     * Toutes les notifications IN_APP non-lues d'un Superviseur, triées created_at DESC.
     * LIMIT 100 : garde-fou technique uniquement — jamais censé être atteint en usage normal.
     * Si ce seuil est atteint en pratique, c'est un signal produit à investiguer.
     */
    @Query(value = """
            SELECT e.* FROM envoi_notification e
            JOIN notification n ON e.id_notification = n.id_notification
            WHERE e.id_superviseur = :idSuperviseur
              AND e.canal = 'IN_APP'
              AND e.lu = false
              AND e.deleted_at IS NULL
            ORDER BY n.created_at DESC
            LIMIT 100
            """, nativeQuery = true)
    List<EnvoiNotification> findNonLuesPourPanel(
            @Param("idSuperviseur") UUID idSuperviseur
    );

    /**
     * Notifications IN_APP lues récentes (dans les 7 derniers jours), plafonnées à 5,
     * triées created_at DESC.
     * Complète les non-lues dans le panel pour donner un contexte de flux récent.
     */
    @Query(value = """
            SELECT e.* FROM envoi_notification e
            JOIN notification n ON e.id_notification = n.id_notification
            WHERE e.id_superviseur = :idSuperviseur
              AND e.canal = 'IN_APP'
              AND e.lu = true
              AND e.deleted_at IS NULL
              AND n.created_at >= :depuis
            ORDER BY n.created_at DESC
            LIMIT 5
            """, nativeQuery = true)
    List<EnvoiNotification> findLuesRecentesPourPanel(
            @Param("idSuperviseur") UUID idSuperviseur,
            @Param("depuis") LocalDateTime depuis
    );

    /**
     * Compteur des notifications IN_APP non lues pour le badge bell icon.
     * Compte toutes les non-lues sans limite ni fenêtre de rétention — toujours exact.
     */
    @Query("""
            SELECT COUNT(e) FROM EnvoiNotification e
            WHERE e.superviseur.idSuperviseur = :idSuperviseur
              AND e.canal = :canal
              AND e.lu = false
              AND e.deletedAt IS NULL
            """)
    long countNonLues(
            @Param("idSuperviseur") UUID idSuperviseur,
            @Param("canal") Canal canal
    );

    /**
     * Marque toutes les notifications IN_APP non lues comme lues pour un Superviseur.
     */
    @Modifying
    @Query("""
            UPDATE EnvoiNotification e
            SET e.lu = true,
                e.dateLecture = :dateLecture,
                e.updatedAt = :dateLecture
            WHERE e.superviseur.idSuperviseur = :idSuperviseur
              AND e.canal = :canal
              AND e.lu = false
              AND e.deletedAt IS NULL
            """)
    void marquerToutLu(
            @Param("idSuperviseur") UUID idSuperviseur,
            @Param("canal") Canal canal,
            @Param("dateLecture") LocalDateTime dateLecture
    );

    /**
     * Ancienne méthode paginée — conservée pour compatibilité si utilisée ailleurs.
     * Le panel utilise désormais findNonLuesPourPanel + findLuesRecentesPourPanel.
     */
    @Query("""
            SELECT e FROM EnvoiNotification e
            JOIN FETCH e.notification n
            WHERE e.superviseur.idSuperviseur = :idSuperviseur
              AND e.canal = :canal
              AND e.deletedAt IS NULL
            ORDER BY n.createdAt DESC
            """)
    Page<EnvoiNotification> findBySuperviseurAndCanal(
            @Param("idSuperviseur") UUID idSuperviseur,
            @Param("canal") Canal canal,
            Pageable pageable
    );
}
