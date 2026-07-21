package com.projet.config.repository;

import com.projet.config.model.ConfigurationPLC;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationPLCRepository extends JpaRepository<ConfigurationPLC, UUID> {
    Optional<ConfigurationPLC> findByActifTrue();
}
