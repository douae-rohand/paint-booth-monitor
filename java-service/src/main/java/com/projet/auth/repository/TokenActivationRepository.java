package com.projet.auth.repository;

import com.projet.auth.model.TokenActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.projet.auth.model.Superviseur;

public interface TokenActivationRepository extends JpaRepository<TokenActivation, UUID> {
    @Query("SELECT t FROM TokenActivation t WHERE t.tokenHash = :tokenHash AND t.utilise = false AND t.dateExpiration > :maintenant")
    Optional<TokenActivation> findByTokenHashAndUtiliseFalseAndDateExpirationAfter(@Param("tokenHash") String tokenHash, @Param("maintenant") LocalDateTime maintenant);

    Optional<TokenActivation> findTopBySuperviseurIdSuperviseurOrderByCreatedAtDesc(UUID idSuperviseur);

    List<TokenActivation> findBySuperviseurAndUtiliseFalse(Superviseur superviseur);
}