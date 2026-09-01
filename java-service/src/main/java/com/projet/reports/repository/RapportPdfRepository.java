package com.projet.reports.repository;

import com.projet.reports.model.RapportPDF;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour l'entité RapportPDF.
 * Module: reports
 *
 * findBySuperviseurIdSuperviseur aurait fonctionné mais est illisible.
 * On utilise une @Query JPQL explicite pour rester clair.
 * La PK de Superviseur s'appelle idSuperviseur (pas id) — d'où l'erreur
 * "No property 'id' found for type 'Superviseur'" au démarrage.
 */
@Repository
public interface RapportPdfRepository extends JpaRepository<RapportPDF, UUID> {

    /**
     * Récupère les rapports PDF d'un superviseur (pagination).
     * Utilisé par le controller pour les non-admins (uniquement leurs propres rapports).
     */
    @Query("SELECT r FROM RapportPDF r WHERE r.superviseur.idSuperviseur = :idSuperviseur")
    Page<RapportPDF> findBySuperviseurIdSuperviseur(
            @Param("idSuperviseur") UUID idSuperviseur,
            Pageable pageable
    );
}
