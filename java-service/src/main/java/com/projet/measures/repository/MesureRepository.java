package com.projet.measures.repository;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.model.Mesure;
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
}
