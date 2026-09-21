package com.projet.alerting.repository;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeuilAbsoluRepository extends JpaRepository<SeuilAbsolu, UUID> {
    Optional<SeuilAbsolu> findByPointMesureIdAndMetriqueAndActifTrue(Long pointMesureId, Metrique metrique);
    List<SeuilAbsolu> findByPointMesureIdAndMetriqueOrderByCreatedAtDesc(Long pointMesureId, Metrique metrique);

    @Modifying
    @Query("UPDATE SeuilAbsolu s SET s.actif = false, s.dateDesactivation = CURRENT_TIMESTAMP WHERE s.pointMesure.id = :pointMesureId AND s.metrique = :metrique AND s.actif = true")
    void deactivateAllActiveForPointMesureAndMetrique(@Param("pointMesureId") Long pointMesureId, @Param("metrique") Metrique metrique);

    /**
     * Récupère le seuil absolu qui était actif à un horodatage précis pour un point de mesure et une métrique.
     * (date_activation <= timestamp ET (date_desactivation IS NULL OU date_desactivation > timestamp))
     */
    @Query("""
        SELECT s FROM SeuilAbsolu s 
        WHERE s.pointMesure.id = :pointMesureId 
          AND s.metrique = :metrique 
          AND (s.dateActivation IS NULL OR s.dateActivation <= :timestamp) 
          AND (s.dateDesactivation IS NULL OR s.dateDesactivation > :timestamp) 
        ORDER BY s.dateActivation DESC NULLS LAST, s.createdAt DESC
    """)
    List<SeuilAbsolu> findSeuilAtTimestamp(
        @Param("pointMesureId") Long pointMesureId, 
        @Param("metrique") Metrique metrique, 
        @Param("timestamp") LocalDateTime timestamp
    );
}
