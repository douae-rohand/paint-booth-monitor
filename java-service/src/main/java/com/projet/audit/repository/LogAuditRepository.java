package com.projet.audit.repository;

import com.projet.audit.model.LogAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogAuditRepository extends JpaRepository<LogAudit, UUID> {

    /**
     * Recherche paginée avec filtres optionnels — requête native PostgreSQL.
     *
     * Même pattern que AlerteRepository.findAlertesNative :
     * CAST explicite sur chaque paramètre pour éviter l'erreur 42P18
     * (PostgreSQL ne peut pas inférer le type d'un paramètre null).
     *
     * Filtre actions (IN) :
     *   - null ou liste vide → CAST(:actionsEmpty AS boolean) = true → pas de filtre
     *   - liste non vide     → l.action = ANY(CAST(:actions AS varchar[]))
     */
    @Query(value = """
        SELECT l.* FROM log_audit l
        WHERE (CAST(:idSuperviseur AS uuid) IS NULL
               OR l.id_superviseur = CAST(:idSuperviseur AS uuid))
          AND (CAST(:actionsEmpty AS boolean) = true
               OR l.action = ANY(CAST(:actions AS varchar[])))
          AND (CAST(:dateDebut AS timestamp) IS NULL
               OR l.date_action >= CAST(:dateDebut AS timestamp))
          AND (CAST(:dateFin AS timestamp) IS NULL
               OR l.date_action <= CAST(:dateFin AS timestamp))
        ORDER BY l.date_action DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM log_audit l
        WHERE (CAST(:idSuperviseur AS uuid) IS NULL
               OR l.id_superviseur = CAST(:idSuperviseur AS uuid))
          AND (CAST(:actionsEmpty AS boolean) = true
               OR l.action = ANY(CAST(:actions AS varchar[])))
          AND (CAST(:dateDebut AS timestamp) IS NULL
               OR l.date_action >= CAST(:dateDebut AS timestamp))
          AND (CAST(:dateFin AS timestamp) IS NULL
               OR l.date_action <= CAST(:dateFin AS timestamp))
        """,
        nativeQuery = true)
    Page<LogAudit> findLogs(
        @Param("idSuperviseur") UUID idSuperviseur,
        @Param("actions") String[] actions,
        @Param("actionsEmpty") boolean actionsEmpty,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        Pageable pageable
    );
}
