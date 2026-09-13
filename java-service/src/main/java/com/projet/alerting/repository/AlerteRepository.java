package com.projet.alerting.repository;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Metrique;
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
     * Compte les alertes ACTIVES en ce moment pour un point de mesure et une métrique.
     * Instantané indépendant de toute période — compte toutes les alertes de statut ACTIVE,
     * quelle que soit leur date de création.
     *
     * <p>Distinct de l'ancienne variante avec filtre dateDebut/dateFin (supprimée) qui
     * sous-comptait les alertes créées avant la fenêtre de période sélectionnée.
     *
     * <p>Utilisé par KpiService.getKpisParPoint pour le champ alertesActives du DTO,
     * qui est affiché comme indicateur "en direct" dans KpiSection.
     */
    @Query("""
            SELECT COUNT(a) FROM Alerte a
            JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure
            WHERE m.pointMesure.id = :idPointMesure
              AND a.metrique = :metrique
              AND a.statut = com.projet.alerting.model.enums.StatutAlerte.ACTIVE
              AND a.deletedAt IS NULL
            """)
    long countAlertesActivesInstantane(
            @Param("idPointMesure") Long idPointMesure,
            @Param("metrique") Metrique metrique
    );

    /**
     * Récupère les alertes SEUIL_ABSOLU filtrées par point de mesure ET métrique sur une période.
     * Utilisé par KpiService pour calculer MTBI et MTTR correctement scopés.
     * La jointure Alerte → Mesure → PointMesure est nécessaire car Alerte ne porte
     * pas directement l'id_point_mesure.
     */
    @Query("""
            SELECT a FROM Alerte a
            JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure
            WHERE m.pointMesure.id = :idPointMesure
              AND a.metrique = :metrique
              AND a.typeAlerte = :typeAlerte
              AND a.createdAt BETWEEN :dateDebut AND :dateFin
              AND a.deletedAt IS NULL
            ORDER BY a.createdAt ASC
            """)
    List<Alerte> findByPointMesureAndMetriqueAndTypeAlerteAndPeriode(
            @Param("idPointMesure") Long idPointMesure,
            @Param("metrique") Metrique metrique,
            @Param("typeAlerte") TypeAlerte typeAlerte,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Récupère les alertes RESOLUES filtrées par point de mesure ET métrique sur une période.
     * Utilisé par KpiService pour calculer MTTR correctement scopé.
     */
    @Query("""
            SELECT a FROM Alerte a
            JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure
            WHERE m.pointMesure.id = :idPointMesure
              AND a.metrique = :metrique
              AND a.statut = :statut
              AND a.createdAt BETWEEN :dateDebut AND :dateFin
              AND a.deletedAt IS NULL
            """)
    List<Alerte> findByPointMesureAndMetriqueAndStatutAndPeriode(
            @Param("idPointMesure") Long idPointMesure,
            @Param("metrique") Metrique metrique,
            @Param("statut") StatutAlerte statut,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Récupère les alertes RESOLUES filtrées par point de mesure ET métrique,
     * dont la <em>date de résolution</em> (updatedAt) tombe dans la période.
     *
     * <p>Sémantique MTTR correcte : on mesure les résolutions survenues pendant la période,
     * indépendamment du moment où l'alerte a été créée. Une alerte créée avant dateDebut
     * mais résolue pendant la période est incluse ; une alerte créée dans la période mais
     * résolue après dateFin est exclue.
     *
     * <p>Différence avec {@link #findByPointMesureAndMetriqueAndStatutAndPeriode} :
     * ce dernier filtre par createdAt (utilisé pour d'autres contextes).
     */
    @Query("""
            SELECT a FROM Alerte a
            JOIN com.projet.measures.model.Mesure m ON a.idMesure = m.idMesure
            WHERE m.pointMesure.id = :idPointMesure
              AND a.metrique = :metrique
              AND a.statut = com.projet.alerting.model.enums.StatutAlerte.RESOLUE
              AND a.updatedAt BETWEEN :dateDebut AND :dateFin
              AND a.deletedAt IS NULL
            """)
    List<Alerte> findAlertesResoluesParPeriodeResolution(
            @Param("idPointMesure") Long idPointMesure,
            @Param("metrique") Metrique metrique,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

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
     * Compte les alertes par type pour un point de mesure et une période.
     * Retourne une liste de [type_alerte, count].
     */
    @Query(value = """
        SELECT a.type_alerte, COUNT(*)
        FROM alerte a
        JOIN mesure m ON a.id_mesure = m.id_mesure
        JOIN point_mesure pm ON m.id_point_mesure = pm.id
        WHERE a.deleted_at IS NULL
        AND pm.id = CAST(:idPointMesure AS bigint)
        AND a.created_at BETWEEN CAST(:dateDebut AS timestamp) AND CAST(:dateFin AS timestamp)
        GROUP BY a.type_alerte
        """, nativeQuery = true)
    List<Object[]> countAlertesByTypeForPointAndPeriod(
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
