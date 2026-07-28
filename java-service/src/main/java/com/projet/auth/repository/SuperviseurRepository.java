package com.projet.auth.repository;

import com.projet.auth.model.Superviseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SuperviseurRepository extends JpaRepository<Superviseur, UUID> {
    Optional<Superviseur> findByEmail(String email);
    boolean existsByAdminIsNotNull();

    @Query("SELECT s FROM Superviseur s LEFT JOIN s.admin a WHERE a IS NULL ORDER BY s.createdAt DESC")
    Page<Superviseur> findByAdminIsNull(Pageable pageable);

    @Query("SELECT s FROM Superviseur s LEFT JOIN s.admin a WHERE a IS NULL AND s.actif = :actif ORDER BY s.createdAt DESC")
    Page<Superviseur> findByAdminIsNullAndActif(@Param("actif") boolean actif, Pageable pageable);

    @Query("SELECT s FROM Superviseur s LEFT JOIN s.admin a WHERE a IS NULL AND s.compteActive = :compteActive ORDER BY s.createdAt DESC")
    Page<Superviseur> findByAdminIsNullAndCompteActive(@Param("compteActive") boolean compteActive, Pageable pageable);

    @Query("SELECT s FROM Superviseur s LEFT JOIN s.admin a WHERE a IS NULL AND s.actif = :actif AND s.compteActive = :compteActive ORDER BY s.createdAt DESC")
    Page<Superviseur> findByAdminIsNullAndActifAndCompteActive(@Param("actif") boolean actif, @Param("compteActive") boolean compteActive, Pageable pageable);

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Superviseur s WHERE s.email = :email AND s.idSuperviseur <> :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") UUID id);
}
