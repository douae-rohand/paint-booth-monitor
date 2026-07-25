package com.projet.measures.repository;

import com.projet.measures.model.PointMesure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité PointMesure.
 */
@Repository
public interface PointMesureRepository extends JpaRepository<PointMesure, Long> {

    /**
     * Retourne tous les points de mesure actifs.
     */
    List<PointMesure> findAllByActifTrue();

    /**
     * Retourne les points de mesure par type d'emplacement.
     * Utile pour lister les 5 zones de l'étuve séparément de la cabine.
     *
     * @param typeEmplacement Type d'emplacement ('CABINE' ou 'ETUVE')
     * @return Liste des points de mesure du type spécifié
     */
    List<PointMesure> findByTypeEmplacement(String typeEmplacement);

    /**
     * Retourne un point de mesure par nom.
     *
     * @param nom Nom du point de mesure
     * @return Optional contenant le point de mesure s'il existe
     */
    Optional<PointMesure> findByNom(String nom);
}
