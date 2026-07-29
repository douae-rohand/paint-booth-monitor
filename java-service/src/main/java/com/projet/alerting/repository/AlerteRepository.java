package com.projet.alerting.repository;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.StatutAlerte;
import com.projet.alerting.model.enums.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
public interface AlerteRepository extends JpaRepository<Alerte, UUID> {

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
}
