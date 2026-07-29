package com.projet.alerting.repository;

import com.projet.alerting.model.SeuilDynamique;
import com.projet.alerting.model.enums.Metrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeuilDynamiqueRepository extends JpaRepository<SeuilDynamique, UUID> {
    Optional<SeuilDynamique> findByPointMesureIdAndMetrique(Long pointMesureId, Metrique metrique);
    Optional<SeuilDynamique> findByPointMesureIdAndMetriqueAndDeletedAtIsNull(Long pointMesureId, Metrique metrique);
    boolean existsByPointMesureIdAndMetrique(Long pointMesureId, Metrique metrique);
}
