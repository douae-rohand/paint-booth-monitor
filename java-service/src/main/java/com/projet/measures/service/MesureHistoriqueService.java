package com.projet.measures.service;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.measures.dto.MesureHistoriqueDTO;
import com.projet.measures.dto.MesureHistoriqueResponseDTO;
import com.projet.measures.exception.MetriqueNonApplicableAuPointException;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour l'historique des mesures.
 * Module: measures
 */
@Service
@RequiredArgsConstructor
public class MesureHistoriqueService {

    private final MesureRepository mesureRepository;
    private final PointMesureRepository pointMesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;

    /**
     * Récupère l'historique des mesures pour un point de mesure et une métrique sur une période.
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique demandée
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @return MesureHistoriqueResponseDTO avec les points et le seuil absolu actif
     * @throws MetriqueNonApplicableAuPointException si la métrique n'est pas applicable au point
     */
    public MesureHistoriqueResponseDTO getHistorique(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Valider que le PointMesure existe
        PointMesure pointMesure = pointMesureRepository.findById(idPointMesure)
                .orElseThrow(() -> new IllegalArgumentException("Point de mesure non trouvé avec ID: " + idPointMesure));

        // Valider que la métrique est applicable au point
        validerMetriqueApplicable(pointMesure, metrique);

        // Récupérer les mesures plausibles dans la période
        List<Mesure> mesures = mesureRepository.findByIdPointMesureAndMetriqueAndCreatedAtBetweenAndPlausibleTrue(
                idPointMesure, metrique, dateDebut, dateFin);

        // Convertir en DTOs
        List<MesureHistoriqueDTO> points = mesures.stream()
                .map(m -> new MesureHistoriqueDTO(m.getCreatedAt(), m.getValeur()))
                .collect(Collectors.toList());

        // Récupérer le seuil absolu actif pour ce point et cette métrique
        SeuilAbsolu seuilAbsolu = seuilAbsoluRepository
                .findByPointMesureIdAndMetriqueAndActifTrue(idPointMesure, metrique)
                .orElse(null);

        MesureHistoriqueResponseDTO.SeuilAbsoluDTO seuilAbsoluDTO = null;
        if (seuilAbsolu != null) {
            seuilAbsoluDTO = new MesureHistoriqueResponseDTO.SeuilAbsoluDTO(
                    seuilAbsolu.getValeurMin(),
                    seuilAbsolu.getValeurMax()
            );
        }

        return new MesureHistoriqueResponseDTO(points, seuilAbsoluDTO);
    }

    /**
     * Valide qu'une métrique est applicable à un point de mesure.
     * CABINE → [TEMPERATURE, HUMIDITE]
     * ETUVE → [TEMPERATURE]
     *
     * @param pointMesure Point de mesure
     * @param metrique Métrique à valider
     * @throws MetriqueNonApplicableAuPointException si non applicable
     */
    private void validerMetriqueApplicable(PointMesure pointMesure, Metrique metrique) {
        String typeEmplacement = pointMesure.getTypeEmplacement();

        if ("ETUVE".equalsIgnoreCase(typeEmplacement) && metrique == Metrique.HUMIDITE) {
            throw new MetriqueNonApplicableAuPointException(
                    String.format("La métrique %s n'est pas applicable au point de mesure %s (type: %s)",
                            metrique, pointMesure.getNom(), typeEmplacement)
            );
        }
    }
}
