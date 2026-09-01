package com.projet.measures.repository;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.model.Mesure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité Mesure.
 * Module: measures
 * 
 * IMPORTANT: Ce repository est en LECTURE SEULE.
 * L'écriture dans la table mesure est réservée au service Python.
 */
@Repository
public interface MesureRepository extends JpaRepository<Mesure, UUID> {

    /**
     * Récupère les mesures pour un point de mesure et une métrique dans une période.
     * Filtre uniquement les mesures plausibles.
     */
    @Query("SELECT m FROM Mesure m WHERE m.pointMesure.id = :idPointMesure AND m.metrique = :metrique AND m.createdAt BETWEEN :dateDebut AND :dateFin AND m.plausible = true ORDER BY m.createdAt ASC")
    List<Mesure> findByIdPointMesureAndMetriqueAndCreatedAtBetweenAndPlausibleTrue(
        @Param("idPointMesure") Long idPointMesure,
        @Param("metrique") Metrique metrique,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Récupère la dernière mesure plausible pour un point de mesure et une métrique.
     */
    @Query("SELECT m FROM Mesure m WHERE m.pointMesure.id = :idPointMesure AND m.metrique = :metrique AND m.plausible = true ORDER BY m.createdAt DESC")
    List<Mesure> findTopByIdPointMesureAndMetriqueAndPlausibleTrueOrderByCreatedAtDesc(
        @Param("idPointMesure") Long idPointMesure,
        @Param("metrique") Metrique metrique
    );

    /**
     * Compte le nombre de mesures plausibles pour un point de mesure et une métrique dans une période.
     */
    @Query("SELECT COUNT(m) FROM Mesure m WHERE m.pointMesure.id = :idPointMesure AND m.metrique = :metrique AND m.createdAt BETWEEN :dateDebut AND :dateFin AND m.plausible = true")
    long countByIdPointMesureAndMetriqueAndCreatedAtBetweenAndPlausibleTrue(
        @Param("idPointMesure") Long idPointMesure,
        @Param("metrique") Metrique metrique,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Récupère une mesure avec son point de mesure chargé (JOIN FETCH) pour éviter LazyInitializationException.
     */
    @Query("SELECT m FROM Mesure m JOIN FETCH m.pointMesure WHERE m.id = :id")
    Mesure findByIdWithPointMesure(@Param("id") UUID id);

    /**
     * Récupère l'historique des mesures de la cabine avec pivot température/humidité par cycle.
     * Le badge dépassement est calculé par comparaison directe aux bornes du SeuilAbsolu
     * qui était ACTIF au moment de la mesure (fenêtre temporelle historisée), sans passer
     * par la table alerte (qui est dédupliquée par épisode, pas par mesure).
     *
     * @param idPointMesure ID du point de mesure de la cabine
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @param limit Taille de page
     * @param offset Décalage de pagination
     * @return Liste d'Object[] contenant les données pivotées
     */
    @Query(value = """
        WITH mesures_pivot AS (
            SELECT
                m.created_at AS timestamp_cycle,
                MAX(CASE WHEN m.metrique = 'TEMPERATURE' THEN m.valeur END) AS temperature_value,
                MAX(CASE WHEN m.metrique = 'TEMPERATURE' THEN m.identifiant_caisse END) AS caisse_id,
                MAX(CASE WHEN m.metrique = 'HUMIDITE' THEN m.valeur END) AS humidite_value
            FROM mesure m
            WHERE m.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND m.plausible = true
                AND (CAST(:dateDebut AS timestamp) IS NULL OR m.created_at >= CAST(:dateDebut AS timestamp))
                AND (CAST(:dateFin AS timestamp) IS NULL OR m.created_at <= CAST(:dateFin AS timestamp))
            GROUP BY m.created_at
        )
        SELECT
            mp.timestamp_cycle,
            mp.caisse_id,
            mp.temperature_value AS temperature,
            mp.humidite_value AS humidite,
            (
                seuil_temp.valeur_min IS NOT NULL
                AND mp.temperature_value IS NOT NULL
                AND (mp.temperature_value < seuil_temp.valeur_min OR mp.temperature_value > seuil_temp.valeur_max)
            ) AS depassement_temperature,
            (
                seuil_hum.valeur_min IS NOT NULL
                AND mp.humidite_value IS NOT NULL
                AND (mp.humidite_value < seuil_hum.valeur_min OR mp.humidite_value > seuil_hum.valeur_max)
            ) AS depassement_humidite
        FROM mesures_pivot mp
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND sa.metrique = 'TEMPERATURE'
                AND sa.date_activation <= mp.timestamp_cycle
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > mp.timestamp_cycle)
            LIMIT 1
        ) seuil_temp ON true
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND sa.metrique = 'HUMIDITE'
                AND sa.date_activation <= mp.timestamp_cycle
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > mp.timestamp_cycle)
            LIMIT 1
        ) seuil_hum ON true
        WHERE (
            CAST(:seulementDepassements AS boolean) = false
            OR (
                seuil_temp.valeur_min IS NOT NULL
                AND mp.temperature_value IS NOT NULL
                AND (mp.temperature_value < seuil_temp.valeur_min OR mp.temperature_value > seuil_temp.valeur_max)
            )
            OR (
                seuil_hum.valeur_min IS NOT NULL
                AND mp.humidite_value IS NOT NULL
                AND (mp.humidite_value < seuil_hum.valeur_min OR mp.humidite_value > seuil_hum.valeur_max)
            )
        )
        ORDER BY mp.timestamp_cycle DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findHistoriqueCabine(
        @Param("idPointMesure") Long idPointMesure,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("seulementDepassements") boolean seulementDepassements,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Compte le nombre total de lignes pour l'historique cabine (pour la pagination).
     * Même logique SeuilAbsolu historisé que findHistoriqueCabine.
     */
    @Query(value = """
        WITH mesures_pivot AS (
            SELECT
                m.created_at AS timestamp_cycle,
                MAX(CASE WHEN m.metrique = 'TEMPERATURE' THEN m.valeur END) AS temperature_value,
                MAX(CASE WHEN m.metrique = 'HUMIDITE' THEN m.valeur END) AS humidite_value
            FROM mesure m
            WHERE m.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND m.plausible = true
                AND (CAST(:dateDebut AS timestamp) IS NULL OR m.created_at >= CAST(:dateDebut AS timestamp))
                AND (CAST(:dateFin AS timestamp) IS NULL OR m.created_at <= CAST(:dateFin AS timestamp))
            GROUP BY m.created_at
        )
        SELECT COUNT(*)
        FROM mesures_pivot mp
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND sa.metrique = 'TEMPERATURE'
                AND sa.date_activation <= mp.timestamp_cycle
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > mp.timestamp_cycle)
            LIMIT 1
        ) seuil_temp ON true
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = CAST(:idPointMesure AS bigint)
                AND sa.metrique = 'HUMIDITE'
                AND sa.date_activation <= mp.timestamp_cycle
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > mp.timestamp_cycle)
            LIMIT 1
        ) seuil_hum ON true
        WHERE (
            CAST(:seulementDepassements AS boolean) = false
            OR (
                seuil_temp.valeur_min IS NOT NULL
                AND mp.temperature_value IS NOT NULL
                AND (mp.temperature_value < seuil_temp.valeur_min OR mp.temperature_value > seuil_temp.valeur_max)
            )
            OR (
                seuil_hum.valeur_min IS NOT NULL
                AND mp.humidite_value IS NOT NULL
                AND (mp.humidite_value < seuil_hum.valeur_min OR mp.humidite_value > seuil_hum.valeur_max)
            )
        )
        """, nativeQuery = true)
    long countHistoriqueCabine(
        @Param("idPointMesure") Long idPointMesure,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("seulementDepassements") boolean seulementDepassements
    );

    /**
     * Récupère l'historique des mesures de l'étuve par zone.
     * Requête native simple sans pivot (une ligne = une mesure).
     * Le badge dépassement est calculé par comparaison directe aux bornes du SeuilAbsolu
     * actif au moment de la mesure (fenêtre temporelle historisée), sans passer par alerte.
     *
     * @param idsZones Liste des IDs des points de mesure des zones (null pour toutes zones)
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @param limit Taille de page
     * @param offset Décalage de pagination
     * @return Liste d'Object[] (id_mesure, date_mesure, zone, temperature, depassement)
     */
    @Query(value = """
        SELECT
            m.id_mesure AS id_mesure,
            m.created_at AS date_mesure,
            pm.nom AS zone,
            m.valeur AS temperature,
            (
                seuil.valeur_min IS NOT NULL
                AND (m.valeur < seuil.valeur_min OR m.valeur > seuil.valeur_max)
            ) AS depassement
        FROM mesure m
        JOIN point_mesure pm ON m.id_point_mesure = pm.id
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = m.id_point_mesure
                AND sa.metrique = 'TEMPERATURE'
                AND sa.date_activation <= m.created_at
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > m.created_at)
            LIMIT 1
        ) seuil ON true
        WHERE (:idsZones IS NULL OR m.id_point_mesure = ANY(CAST(:idsZones AS bigint[])))
            AND m.plausible = true
            AND m.metrique = 'TEMPERATURE'
            AND (CAST(:dateDebut AS timestamp) IS NULL OR m.created_at >= CAST(:dateDebut AS timestamp))
            AND (CAST(:dateFin AS timestamp) IS NULL OR m.created_at <= CAST(:dateFin AS timestamp))
            AND (
                CAST(:seulementDepassements AS boolean) = false
                OR (
                    seuil.valeur_min IS NOT NULL
                    AND (m.valeur < seuil.valeur_min OR m.valeur > seuil.valeur_max)
                )
            )
        ORDER BY m.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findHistoriqueEtuve(
        @Param("idsZones") Long[] idsZones,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("seulementDepassements") boolean seulementDepassements,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Compte le nombre total de lignes pour l'historique étuve (pour la pagination).
     * Même logique SeuilAbsolu historisé que findHistoriqueEtuve.
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM mesure m
        JOIN point_mesure pm ON m.id_point_mesure = pm.id
        LEFT JOIN LATERAL (
            SELECT sa.valeur_min, sa.valeur_max
            FROM seuil_absolu sa
            WHERE sa.id_point_mesure = m.id_point_mesure
                AND sa.metrique = 'TEMPERATURE'
                AND sa.date_activation <= m.created_at
                AND (sa.date_desactivation IS NULL OR sa.date_desactivation > m.created_at)
            LIMIT 1
        ) seuil ON true
        WHERE (:idsZones IS NULL OR m.id_point_mesure = ANY(CAST(:idsZones AS bigint[])))
            AND m.plausible = true
            AND m.metrique = 'TEMPERATURE'
            AND (CAST(:dateDebut AS timestamp) IS NULL OR m.created_at >= CAST(:dateDebut AS timestamp))
            AND (CAST(:dateFin AS timestamp) IS NULL OR m.created_at <= CAST(:dateFin AS timestamp))
            AND (
                CAST(:seulementDepassements AS boolean) = false
                OR (
                    seuil.valeur_min IS NOT NULL
                    AND (m.valeur < seuil.valeur_min OR m.valeur > seuil.valeur_max)
                )
            )
        """, nativeQuery = true)
    long countHistoriqueEtuve(
        @Param("idsZones") Long[] idsZones,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("seulementDepassements") boolean seulementDepassements
    );

    /**
     * Calcule les statistiques (min, max, moyenne) pour un point de mesure et une métrique sur une période.
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Object[] contenant [min, max, moyenne] (BigDecimal) ou null si aucune donnée
     */
    @Query(value = """
        SELECT
            MIN(m.valeur) AS min_val,
            MAX(m.valeur) AS max_val,
            AVG(m.valeur) AS avg_val
        FROM mesure m
        WHERE m.id_point_mesure = CAST(:idPointMesure AS bigint)
            AND m.metrique = CAST(:metrique AS varchar)
            AND m.plausible = true
            AND m.created_at BETWEEN CAST(:dateDebut AS timestamp) AND CAST(:dateFin AS timestamp)
        """, nativeQuery = true)
    Object[] calculateStatisticsForPointAndPeriod(
        @Param("idPointMesure") Long idPointMesure,
        @Param("metrique") String metrique,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin
    );
}
