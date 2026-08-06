package com.projet.alerting.service;

import com.projet.alerting.dto.AlerteDTO;
import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Severite;
import com.projet.alerting.model.enums.StatutAlerte;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.measures.model.Mesure;
import com.projet.measures.repository.MesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service pour la consultation des alertes.
 * Module: alerting
 */
@Service
@RequiredArgsConstructor
public class AlerteConsultationService {

    private final AlerteRepository alerteRepository;
    private final MesureRepository mesureRepository;

    /**
     * Récupère l'historique des alertes avec filtres optionnels et pagination.
     *
     * @param statut Filtre par statut (optionnel)
     * @param typeAlerte Filtre par type d'alerte (optionnel)
     * @param severite Filtre par sévérité (optionnel)
     * @param idPointMesure Filtre par ID du point de mesure (optionnel)
     * @param dateDebut Filtre par date de début (optionnel)
     * @param dateFin Filtre par date de fin (optionnel)
     * @param pageable Pagination
     * @return Page d'AlerteDTO
     */
    public Page<AlerteDTO> getHistoriqueAlertes(
            String statut,
            String typeAlerte,
            String severite,
            Long idPointMesure,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    ) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<Alerte> alertes = alerteRepository.findAlertesNative(
                statut,
                typeAlerte,
                severite,
                idPointMesure,
                dateDebut,
                dateFin,
                limit,
                offset
        );

        long total = alerteRepository.countAlertesNative(
                statut,
                typeAlerte,
                severite,
                idPointMesure,
                dateDebut,
                dateFin
        );

        List<AlerteDTO> dtos = alertes.stream()
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, total);
    }

    /**
     * Récupère toutes les alertes actives (sans pagination).
     *
     * @return Liste d'AlerteDTO
     */
    public List<AlerteDTO> getAlertesActives() {
        List<Alerte> alertes = alerteRepository.findAlertesActives();
        return alertes.stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Convertit une entité Alerte en AlerteDTO.
     *
     * @param alerte Entité Alerte
     * @return AlerteDTO
     */
    private AlerteDTO mapToDTO(Alerte alerte) {
        // Récupérer la mesure liée pour obtenir le nom du point de mesure
        Mesure mesure = mesureRepository.findById(alerte.getIdMesure()).orElse(null);
        String pointMesureNom = mesure != null && mesure.getPointMesure() != null
                ? mesure.getPointMesure().getNom()
                : "Inconnu";

        // Calculer la durée en minutes
        LocalDateTime dateResolution = alerte.getUpdatedAt();
        LocalDateTime dateFin = dateResolution != null ? dateResolution : LocalDateTime.now();
        long dureeMinutes = ChronoUnit.MINUTES.between(alerte.getCreatedAt(), dateFin);

        return new AlerteDTO(
                alerte.getIdAlerte(),
                alerte.getCreatedAt(),
                dateResolution,
                pointMesureNom,
                alerte.getMetrique().name(),
                alerte.getTypeAlerte().name(),
                alerte.getSeverite().name(),
                alerte.getStatut().name(),
                dureeMinutes
        );
    }
}
