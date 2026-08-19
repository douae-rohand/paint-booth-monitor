package com.projet.auth.repository;

import com.projet.auth.model.TokenReinitialisation;
import com.projet.auth.model.Superviseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenReinitialisationRepository extends JpaRepository<TokenReinitialisation, UUID> {
    
    @Query("SELECT t FROM TokenReinitialisation t WHERE t.tokenHash = :tokenHash AND t.utilise = false AND t.dateExpiration > :maintenant")
    Optional<TokenReinitialisation> findByTokenHashAndUtiliseFalseAndDateExpirationAfter(
            @Param("tokenHash") String tokenHash,
            @Param("maintenant") LocalDateTime maintenant
    );

    List<TokenReinitialisation> findBySuperviseur(Superviseur superviseur);
}
