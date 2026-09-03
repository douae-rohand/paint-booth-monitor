package com.projet.notifications.repository;

import com.projet.notifications.model.AbonnementPushNavigateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les abonnements Web Push (VAPID).
 * Module: notifications
 */
@Repository
public interface AbonnementPushNavigateurRepository extends JpaRepository<AbonnementPushNavigateur, UUID> {

    /**
     * Récupère tous les abonnements actifs d'un superviseur.
     *
     * @param idSuperviseur ID du superviseur
     * @return Liste des abonnements
     */
    List<AbonnementPushNavigateur> findBySuperviseur_IdSuperviseur(UUID idSuperviseur);

    /**
     * Recherche un abonnement par son endpoint.
     *
     * @param endpoint URL du service de push
     * @return Optional de l'abonnement
     */
    Optional<AbonnementPushNavigateur> findByEndpoint(String endpoint);

    /**
     * Supprime un abonnement par son endpoint.
     *
     * @param endpoint URL du service de push
     */
    void deleteByEndpoint(String endpoint);
}
