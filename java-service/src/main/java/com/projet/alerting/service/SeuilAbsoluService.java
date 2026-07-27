package com.projet.alerting.service;

import com.projet.alerting.dto.SeuilAbsoluCreateDTO;
import com.projet.alerting.dto.SeuilAbsoluResponseDTO;
import com.projet.alerting.exception.BusinessException;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.auth.model.Admin;
import com.projet.auth.repository.AdminRepository;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.PointMesureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SeuilAbsoluService {

    private final SeuilAbsoluRepository seuilAbsoluRepository;
    private final PointMesureRepository pointMesureRepository;
    private final AdminRepository adminRepository;

    public SeuilAbsoluService(SeuilAbsoluRepository seuilAbsoluRepository,
                              PointMesureRepository pointMesureRepository,
                              AdminRepository adminRepository) {
        this.seuilAbsoluRepository = seuilAbsoluRepository;
        this.pointMesureRepository = pointMesureRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public SeuilAbsoluResponseDTO creer(SeuilAbsoluCreateDTO dto, UUID idAdminConnecte) {
        // Valider PointMesure
        PointMesure pm = pointMesureRepository.findById(dto.getIdPointMesure())
                .orElseThrow(() -> new BusinessException("POINT_MESURE_INACTIF", HttpStatus.BAD_REQUEST));
        if (!pm.isActif() || pm.getDeletedAt() != null) {
            throw new BusinessException("POINT_MESURE_INACTIF", HttpStatus.BAD_REQUEST);
        }

        // Valider valeurMin < valeurMax
        if (dto.getValeurMin() == null || dto.getValeurMax() == null ||
                dto.getValeurMin().compareTo(dto.getValeurMax()) >= 0) {
            throw new BusinessException("VALEUR_MIN_SUPERIEURE_MAX", HttpStatus.BAD_REQUEST);
        }

        // Trouver l'admin connecté
        Admin admin = adminRepository.findById(idAdminConnecte)
                .orElseThrow(() -> new BusinessException("ADMIN_NON_TROUVE", HttpStatus.NOT_FOUND));

        // Désactiver l'ancien seuil actif s'il existe
        seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(dto.getIdPointMesure(), dto.getMetrique())
                .ifPresent(ancien -> {
                    ancien.setActif(false);
                    ancien.setDateDesactivation(LocalDateTime.now());
                    seuilAbsoluRepository.save(ancien);
                });

        // Créer le nouveau seuil
        SeuilAbsolu seuil = new SeuilAbsolu();
        seuil.setAdmin(admin);
        seuil.setPointMesure(pm);
        seuil.setMetrique(dto.getMetrique());
        seuil.setValeurMin(dto.getValeurMin());
        seuil.setValeurMax(dto.getValeurMax());
        seuil.setActif(true);
        seuil.setDateActivation(LocalDateTime.now());
        seuil.setCreatedAt(LocalDateTime.now());

        SeuilAbsolu saved = seuilAbsoluRepository.save(seuil);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public SeuilAbsoluResponseDTO activer(UUID id) {
        SeuilAbsolu seuil = seuilAbsoluRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SEUIL_ABSOLU_NON_TROUVE", HttpStatus.NOT_FOUND));

        if (!seuil.isActif()) {
            // Désactiver l'ancien seuil actif pour le même point de mesure et métrique
            seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(seuil.getPointMesure().getId(), seuil.getMetrique())
                    .ifPresent(ancien -> {
                        if (!ancien.getIdSeuilAbsolu().equals(id)) {
                            ancien.setActif(false);
                            ancien.setDateDesactivation(LocalDateTime.now());
                            seuilAbsoluRepository.save(ancien);
                        }
                    });

            seuil.setActif(true);
            seuil.setDateActivation(LocalDateTime.now());
            seuil.setDateDesactivation(null);
            seuil = seuilAbsoluRepository.save(seuil);
        }

        return mapToResponseDTO(seuil);
    }

    @Transactional
    public SeuilAbsoluResponseDTO desactiver(UUID id) {
        SeuilAbsolu seuil = seuilAbsoluRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SEUIL_ABSOLU_NON_TROUVE", HttpStatus.NOT_FOUND));

        if (seuil.isActif()) {
            seuil.setActif(false);
            seuil.setDateDesactivation(LocalDateTime.now());
            seuil = seuilAbsoluRepository.save(seuil);
        }

        return mapToResponseDTO(seuil);
    }

    @Transactional(readOnly = true)
    public SeuilAbsoluResponseDTO getActive(Long pointMesureId, Metrique metrique) {
        SeuilAbsolu seuil = seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(pointMesureId, metrique)
                .orElseThrow(() -> new BusinessException("SEUIL_ABSOLU_NON_TROUVE", HttpStatus.NOT_FOUND));
        return mapToResponseDTO(seuil);
    }

    @Transactional(readOnly = true)
    public List<SeuilAbsoluResponseDTO> getHistory(Long pointMesureId, Metrique metrique) {
        return seuilAbsoluRepository.findByPointMesureIdAndMetriqueOrderByCreatedAtDesc(pointMesureId, metrique)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private SeuilAbsoluResponseDTO mapToResponseDTO(SeuilAbsolu seuil) {
        return new SeuilAbsoluResponseDTO(
                seuil.getIdSeuilAbsolu(),
                seuil.getPointMesure().getId(),
                seuil.getPointMesure().getNom(),
                seuil.getMetrique(),
                seuil.getValeurMin(),
                seuil.getValeurMax(),
                seuil.isActif(),
                seuil.getCreatedAt(),
                seuil.getDateActivation(),
                seuil.getDateDesactivation()
        );
    }
}
