package com.projet.alerting.service;

import com.projet.alerting.dto.SeuilDynamiqueCreateDTO;
import com.projet.alerting.dto.SeuilDynamiqueResponseDTO;
import com.projet.alerting.dto.SeuilDynamiqueUpdateDTO;
import com.projet.alerting.exception.BusinessException;
import com.projet.alerting.model.SeuilDynamique;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilDynamiqueRepository;
import com.projet.auth.model.Admin;
import com.projet.auth.repository.AdminRepository;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.PointMesureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SeuilDynamiqueService {

    private final SeuilDynamiqueRepository seuilDynamiqueRepository;
    private final PointMesureRepository pointMesureRepository;
    private final AdminRepository adminRepository;

    public SeuilDynamiqueService(SeuilDynamiqueRepository seuilDynamiqueRepository,
                                 PointMesureRepository pointMesureRepository,
                                 AdminRepository adminRepository) {
        this.seuilDynamiqueRepository = seuilDynamiqueRepository;
        this.pointMesureRepository = pointMesureRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public SeuilDynamiqueResponseDTO creer(SeuilDynamiqueCreateDTO dto, UUID idAdminConnecte) {
        // Valider PointMesure
        PointMesure pm = pointMesureRepository.findById(dto.getIdPointMesure())
                .orElseThrow(() -> new BusinessException("POINT_MESURE_INACTIF", HttpStatus.BAD_REQUEST));
        if (!pm.isActif() || pm.getDeletedAt() != null) {
            throw new BusinessException("POINT_MESURE_INACTIF", HttpStatus.BAD_REQUEST);
        }

        // Valider l'unicité
        if (seuilDynamiqueRepository.existsByPointMesureIdAndMetrique(dto.getIdPointMesure(), dto.getMetrique())) {
            throw new BusinessException("SEUIL_DYNAMIQUE_DEJA_EXISTANT", HttpStatus.BAD_REQUEST);
        }

        // Trouver l'admin
        Admin admin = adminRepository.findById(idAdminConnecte)
                .orElseThrow(() -> new BusinessException("ADMIN_NON_TROUVE", HttpStatus.NOT_FOUND));

        // Créer l'entité
        SeuilDynamique seuil = new SeuilDynamique();
        seuil.setAdmin(admin);
        seuil.setPointMesure(pm);
        seuil.setMetrique(dto.getMetrique());
        seuil.setMargeConfiguree(dto.getMargeConfiguree());
        seuil.setCreatedAt(LocalDateTime.now());

        SeuilDynamique saved = seuilDynamiqueRepository.save(seuil);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public SeuilDynamiqueResponseDTO modifierMarge(UUID id, SeuilDynamiqueUpdateDTO dto) {
        SeuilDynamique seuil = seuilDynamiqueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SEUIL_DYNAMIQUE_NON_TROUVE", HttpStatus.NOT_FOUND));

        seuil.setMargeConfiguree(dto.getMargeConfiguree());
        seuil.setUpdatedAt(LocalDateTime.now());
        SeuilDynamique saved = seuilDynamiqueRepository.save(seuil);
        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public SeuilDynamiqueResponseDTO get(Long pointMesureId, Metrique metrique) {
        return seuilDynamiqueRepository.findByPointMesureIdAndMetrique(pointMesureId, metrique)
                .map(this::mapToResponseDTO)
                .orElse(null);
    }

    private SeuilDynamiqueResponseDTO mapToResponseDTO(SeuilDynamique seuil) {
        return new SeuilDynamiqueResponseDTO(
                seuil.getIdSeuilDynamique(),
                seuil.getPointMesure().getId(),
                seuil.getPointMesure().getNom(),
                seuil.getMetrique(),
                seuil.getMargeConfiguree(),
                seuil.getValeurMinCalculee(),
                seuil.getValeurMaxCalculee(),
                seuil.getDateCalcul()
        );
    }
}
