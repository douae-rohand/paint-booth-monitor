package com.projet.measures.service;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.SeuilDynamique;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.alerting.repository.SeuilDynamiqueRepository;
import com.projet.measures.dto.PointMesureStatutDTO;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour le statut temps réel des points de mesure.
 * Module: measures
 */
@Service
@RequiredArgsConstructor
public class StatutTempsReelService {

    private final PointMesureRepository pointMesureRepository;
    private final MesureRepository mesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;
    private final SeuilDynamiqueRepository seuilDynamiqueRepository;

    /**
     * Récupère le statut temps réel de tous les points de mesure actifs.
     *
     * @return Liste des statuts de tous les points
     */
    public List<PointMesureStatutDTO> getStatutTousPoints() {
        // Récupérer tous les points de mesure actifs non supprimés
        List<PointMesure> pointsActifs = pointMesureRepository.findAllByActifTrue();

        List<PointMesureStatutDTO> result = new ArrayList<>();

        for (PointMesure point : pointsActifs) {
            // Déterminer les métriques applicables selon le type d'emplacement
            List<Metrique> metriquesApplicables = getMetriquesApplicables(point.getTypeEmplacement());

            List<PointMesureStatutDTO.MesureStatutDTO> mesuresStatut = new ArrayList<>();

            for (Metrique metrique : metriquesApplicables) {
                PointMesureStatutDTO.MesureStatutDTO statutDTO = calculerStatutMesure(point, metrique);
                mesuresStatut.add(statutDTO);
            }

            PointMesureStatutDTO pointStatut = new PointMesureStatutDTO(
                    point.getId(),
                    point.getNom(),
                    point.getTypeEmplacement(),
                    mesuresStatut
            );

            result.add(pointStatut);
        }

        return result;
    }

    /**
     * Détermine les métriques applicables selon le type d'emplacement.
     * CABINE → [TEMPERATURE, HUMIDITE]
     * ETUVE → [TEMPERATURE]
     *
     * @param typeEmplacement Type d'emplacement
     * @return Liste des métriques applicables
     */
    private List<Metrique> getMetriquesApplicables(String typeEmplacement) {
        List<Metrique> metriques = new ArrayList<>();

        if ("CABINE".equalsIgnoreCase(typeEmplacement)) {
            metriques.add(Metrique.TEMPERATURE);
            metriques.add(Metrique.HUMIDITE);
        } else if ("ETUVE".equalsIgnoreCase(typeEmplacement)) {
            metriques.add(Metrique.TEMPERATURE);
        }

        return metriques;
    }

    /**
     * Calcule le statut d'une mesure pour un point et une métrique.
     *
     * @param point Point de mesure
     * @param metrique Métrique
     * @return MesureStatutDTO avec le statut calculé
     */
    private PointMesureStatutDTO.MesureStatutDTO calculerStatutMesure(PointMesure point, Metrique metrique) {
        // Récupérer la dernière mesure plausible
        List<Mesure> dernieresMesures = mesureRepository.findTopByIdPointMesureAndMetriqueAndPlausibleTrueOrderByCreatedAtDesc(
                point.getId(), metrique);

        if (dernieresMesures.isEmpty()) {
            // Aucune mesure récente
            return new PointMesureStatutDTO.MesureStatutDTO(
                    metrique,
                    null,
                    null,
                    PointMesureStatutDTO.StatutMesure.INCONNU
            );
        }

        Mesure derniereMesure = dernieresMesures.get(0);

        // Récupérer les seuils actifs (non supprimés)
        Optional<SeuilAbsolu> seuilAbsolu = seuilAbsoluRepository
                .findByPointMesureIdAndMetriqueAndActifTrue(point.getId(), metrique);

        Optional<SeuilDynamique> seuilDynamique = seuilDynamiqueRepository
                .findByPointMesureIdAndMetriqueAndDeletedAtIsNull(point.getId(), metrique);

        // Si aucun seuil configuré
        if (seuilAbsolu.isEmpty() && seuilDynamique.isEmpty()) {
            return new PointMesureStatutDTO.MesureStatutDTO(
                    metrique,
                    derniereMesure.getValeur(),
                    derniereMesure.getCreatedAt(),
                    PointMesureStatutDTO.StatutMesure.INCONNU
            );
        }

        BigDecimal valeur = derniereMesure.getValeur();

        // Vérifier CRITIQUE : hors SeuilAbsolu
        if (seuilAbsolu.isPresent()) {
            SeuilAbsolu sa = seuilAbsolu.get();
            if (valeur.compareTo(sa.getValeurMin()) < 0 || valeur.compareTo(sa.getValeurMax()) > 0) {
                return new PointMesureStatutDTO.MesureStatutDTO(
                        metrique,
                        valeur,
                        derniereMesure.getCreatedAt(),
                        PointMesureStatutDTO.StatutMesure.CRITIQUE
                );
            }
        }

        // Vérifier ATTENTION : dans SeuilAbsolu mais hors SeuilDynamique
        if (seuilDynamique.isPresent()) {
            SeuilDynamique sd = seuilDynamique.get();
            if (valeur.compareTo(sd.getValeurMinCalculee()) < 0 || valeur.compareTo(sd.getValeurMaxCalculee()) > 0) {
                return new PointMesureStatutDTO.MesureStatutDTO(
                        metrique,
                        valeur,
                        derniereMesure.getCreatedAt(),
                        PointMesureStatutDTO.StatutMesure.ATTENTION
                );
            }
        }

        // NOMINAL : dans les deux seuils
        return new PointMesureStatutDTO.MesureStatutDTO(
                metrique,
                valeur,
                derniereMesure.getCreatedAt(),
                PointMesureStatutDTO.StatutMesure.NOMINAL
        );
    }
}
