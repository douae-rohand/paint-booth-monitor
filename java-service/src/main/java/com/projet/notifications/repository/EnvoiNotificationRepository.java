package com.projet.notifications.repository;

import com.projet.notifications.model.EnvoiNotification;
import com.projet.notifications.model.enums.Canal;
import com.projet.notifications.model.enums.StatutEnvoi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité EnvoiNotification.
 * Appartient à Java (lecture + écriture). Python n'accède pas à cette table.
 */
public interface EnvoiNotificationRepository extends JpaRepository<EnvoiNotification, UUID> {

    /**
     * Sélectionne le prochain batch d'envois EMAIL en attente de traitement.
     * Utilisé par EmailWorkerService à chaque cycle @Scheduled.
     * Le JOIN FETCH évite les N+1 sur notification et superviseur.
     */
    @Query("""
            SELECT e FROM EnvoiNotification e
            JOIN FETCH e.notification n
            JOIN FETCH e.superviseur s
            WHERE e.canal = :canal
              AND e.statutEnvoi = :statut
              AND e.deletedAt IS NULL
            ORDER BY n.createdAt ASC
            """)
    List<EnvoiNotification> findBatchPourTraitement(
            @Param("canal") Canal canal,
            @Param("statut") StatutEnvoi statut,
            Pageable pageable
    );

    /**
     * Vérifie si une notification a déjà un envoi EMAIL créé pour un superviseur donné.
     * Garde l'idempotence en cas de double NOTIFY PostgreSQL.
     */
    boolean existsByNotification_IdNotificationAndSuperviseur_IdSuperviseurAndCanal(
            UUID idNotification,
            UUID idSuperviseur,
            Canal canal
    );
}
