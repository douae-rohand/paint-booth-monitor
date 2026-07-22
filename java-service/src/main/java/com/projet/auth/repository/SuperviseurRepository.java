package com.projet.auth.repository;

import com.projet.auth.model.Superviseur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SuperviseurRepository extends JpaRepository<Superviseur, UUID> {
    Optional<Superviseur> findByEmail(String email);
    boolean existsByAdminIsNotNull();
}
