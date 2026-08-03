package com.projet.alerting.repository;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeuilAbsoluRepository extends JpaRepository<SeuilAbsolu, UUID> {
    Optional<SeuilAbsolu> findByPointMesureIdAndMetriqueAndActifTrue(Long pointMesureId, Metrique metrique);
    List<SeuilAbsolu> findByPointMesureIdAndMetriqueOrderByCreatedAtDesc(Long pointMesureId, Metrique metrique);

    @Modifying
    @Query("UPDATE SeuilAbsolu s SET s.actif = false, s.dateDesactivation = CURRENT_TIMESTAMP WHERE s.pointMesure.id = :pointMesureId AND s.metrique = :metrique AND s.actif = true")
    void deactivateAllActiveForPointMesureAndMetrique(Long pointMesureId, Metrique metrique);
}
