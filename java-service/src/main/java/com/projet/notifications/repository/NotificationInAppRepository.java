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
import java.util.UUID;

/**
 * Repository dédié aux requêtes IN_APP pour le bell icon.
 * Lit EnvoiNotification JOIN Notification, filtré sur canal=IN_APP et le Superviseur courant.
 */
public interface NotificationInAppRepository extends JpaRepository<EnvoiNotification, UUID> {

    /**
     * Liste paginée des notifications IN_APP pour un Superviseur donné,
     * triée par date de création décroissante.
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

    /**
     * Compteur des notifications IN_APP non lues pour le badge bell icon.
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
     * Utilisé par PATCH /api/notifications/lu-tout.
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
}
