package com.projet.auth.repository;

import com.projet.auth.model.RefreshToken;
import com.projet.auth.model.Superviseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findBySuperviseurAndUserAgentAndRevoqueFalse(Superviseur superviseur, String userAgent);
    List<RefreshToken> findBySuperviseur(Superviseur superviseur);
}
