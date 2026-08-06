package com.projet.alerting.repository;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Severite;
import com.projet.alerting.model.enums.StatutAlerte;
import com.projet.alerting.model.enums.TypeAlerte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité Alerte.
 * Module: alerting
 */
@Repository
public interface AlerteRepository extends JpaRepository<Alerte, UUID>, JpaSpecificationExecutor<Alerte> {

    /**
     * Compte le nombre d'alertes actives.
     */
    long countByStatut(StatutAlerte statut);

    /**
     * Compte le nombre distinct de points de mesure ayant des alertes actives.
     */
    @Query("SELECT COUNT(DISTINCT m.pointMesure.id) FROM Alerte a JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure WHERE a.statut = :statut AND a.deletedAt IS NULL")
    long countDistinctPointMesureByStatut(@Param("statut") StatutAlerte statut);

    /**
     * Récupère les alertes pour une période donnée.
     */
    List<Alerte> findByCreatedAtBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

    /**
     * Récupère les alertes par type et période.
     */
    List<Alerte> findByTypeAlerteAndCreatedAtBetween(TypeAlerte typeAlerte, LocalDateTime dateDebut, LocalDateTime dateFin);

    /**
     * Récupère les alertes résolues dans une période.
     */
    List<Alerte> findByStatutAndCreatedAtBetween(StatutAlerte statut, LocalDateTime dateDebut, LocalDateTime dateFin);

    /**
     * Récupère les alertes avec filtres optionnels et pagination sous forme de requête native.
     */
    @Query(value = """
        SELECT a.* FROM alerte a
        JOIN mesure m ON a.id_mesure = m.id_mesure
        JOIN point_mesure pm ON m.id_point_mesure = pm.id
        WHERE a.deleted_at IS NULL
        AND (CAST(:statut AS varchar) IS NULL OR a.statut = CAST(:statut AS varchar))
        AND (CAST(:typeAlerte AS varchar) IS NULL OR a.type_alerte = CAST(:typeAlerte AS varchar))
        AND (CAST(:severite AS varchar) IS NULL OR a.severite = CAST(:severite AS varchar))
        AND (CAST(:idPointMesure AS bigint) IS NULL OR pm.id = CAST(:idPointMesure AS bigint))
        AND (CAST(:dateDebut AS timestamp) IS NULL OR a.created_at >= CAST(:dateDebut AS timestamp))
        AND (CAST(:dateFin AS timestamp) IS NULL OR a.created_at <= CAST(:dateFin AS timestamp))
        ORDER BY a.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Alerte> findAlertesNative(
        @Param("statut") String statut,
        @Param("typeAlerte") String typeAlerte,
        @Param("severite") String severite,
        @Param("idPointMesure") Long idPointMesure,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Compte le nombre total d'alertes correspondant aux filtres.
     */
    @Query(value = """
        SELECT COUNT(*) FROM alerte a
        JOIN mesure m ON a.id_mesure = m.id_mesure
        JOIN point_mesure pm ON m.id_point_mesure = pm.id
        WHERE a.deleted_at IS NULL
        AND (CAST(:statut AS varchar) IS NULL OR a.statut = CAST(:statut AS varchar))
        AND (CAST(:typeAlerte AS varchar) IS NULL OR a.type_alerte = CAST(:typeAlerte AS varchar))
        AND (CAST(:severite AS varchar) IS NULL OR a.severite = CAST(:severite AS varchar))
        AND (CAST(:idPointMesure AS bigint) IS NULL OR pm.id = CAST(:idPointMesure AS bigint))
        AND (CAST(:dateDebut AS timestamp) IS NULL OR a.created_at >= CAST(:dateDebut AS timestamp))
        AND (CAST(:dateFin AS timestamp) IS NULL OR a.created_at <= CAST(:dateFin AS timestamp))
        """, nativeQuery = true)
    long countAlertesNative(
        @Param("statut") String statut,
        @Param("typeAlerte") String typeAlerte,
        @Param("severite") String severite,
        @Param("idPointMesure") Long idPointMesure,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Récupère toutes les alertes actives (sans pagination).
     * Jointure Alerte -> Mesure -> PointMesure pour obtenir le nom du point de mesure.
     */
    @Query("""
        SELECT a FROM Alerte a
        JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure
        JOIN com.projet.measures.model.PointMesure pm ON m.pointMesure.id = pm.id
        WHERE a.statut = 'ACTIVE'
        AND a.deletedAt IS NULL
        ORDER BY a.createdAt DESC
        """)
    List<Alerte> findAlertesActives();
}
