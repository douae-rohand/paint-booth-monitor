package com.projet.kpis.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.StatutAlerte;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.measures.model.Mesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour le calcul des KPIs.
 * Module: kpis
 */
@Service
@RequiredArgsConstructor
public class KpiService {

    private final AlerteRepository alerteRepository;
    private final PointMesureRepository pointMesureRepository;
    private final MesureRepository mesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;

    /**
     * Récupère les KPIs globaux (indépendants de la période).
     *
     * @return KpiResponseDTO avec les KPIs globaux
     */
    public KpiResponseDTO getKpisGlobaux() {
        long alertesActives = alerteRepository.countByStatut(StatutAlerte.ACTIVE);
        long nbPointsEnAnomalie = alerteRepository.countDistinctPointMesureByStatut(StatutAlerte.ACTIVE);
        long nbPointsTotal = pointMesureRepository.countByActifTrueAndDeletedAtIsNull();

        return new KpiResponseDTO(
                alertesActives,
                nbPointsEnAnomalie,
                nbPointsTotal,
                null,  // tauxConformite non applicable globalement
                null,  // tempsMoyenEntreIncidentsHeures non applicable globalement
                null   // tempsMoyenRetourNormalHeures non applicable globalement
        );
    }

    /**
     * Récupère les KPIs pour un point de mesure et une métrique sur une période.
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @return KpiResponseDTO avec les KPIs scopés
     */
    public KpiResponseDTO getKpisParPoint(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // KPIs scopés par point + métrique + période
        long alertesActives = alerteRepository.countAlertesActivesParPointEtPeriode(idPointMesure, metrique, dateDebut, dateFin);
        long nbPointsEnAnomalie = alerteRepository.countDistinctPointsEnAnomalieParPointEtPeriode(idPointMesure, metrique, dateDebut, dateFin);
        long nbPointsTotal = pointMesureRepository.countByActifTrueAndDeletedAtIsNull();

        // KPIs scopés
        Double tauxConformite = calculerTauxConformite(idPointMesure, metrique, dateDebut, dateFin);
        Double tempsMoyenEntreIncidents = calculerTempsMoyenEntreIncidents(idPointMesure, metrique, dateDebut, dateFin);
        Double tempsMoyenRetourNormal = calculerTempsMoyenRetourNormal(idPointMesure, metrique, dateDebut, dateFin);

        return new KpiResponseDTO(
                alertesActives,
                nbPointsEnAnomalie,
                nbPointsTotal,
                tauxConformite,
                tempsMoyenEntreIncidents,
                tempsMoyenRetourNormal
        );
    }

    /**
     * Calcule le taux de conformité pour un point et une métrique sur une période.
     * Approche pragmatique : ratio (mesures dans les bornes) / (total mesures plausibles).
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Taux de conformité en pourcentage, ou null si aucune donnée ou aucun seuil
     */
    private Double calculerTauxConformite(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Récupérer le seuil absolu actif
        SeuilAbsolu seuilAbsolu = seuilAbsoluRepository
                .findByPointMesureIdAndMetriqueAndActifTrue(idPointMesure, metrique)
                .orElse(null);

        if (seuilAbsolu == null) {
            return null;  // Aucun seuil configuré, impossible de calculer la conformité
        }

        BigDecimal valeurMin = seuilAbsolu.getValeurMin();
        BigDecimal valeurMax = seuilAbsolu.getValeurMax();

        // Récupérer toutes les mesures plausibles dans la période
        List<Mesure> toutesMesures = mesureRepository.findByIdPointMesureAndMetriqueAndCreatedAtBetweenAndPlausibleTrue(
                idPointMesure, metrique, dateDebut, dateFin);

        if (toutesMesures.isEmpty()) {
            return null;  // Aucune donnée sur la période
        }

        // Compter les mesures dans les bornes
        long mesuresDansBornes = toutesMesures.stream()
                .filter(m -> m.getValeur().compareTo(valeurMin) >= 0 && m.getValeur().compareTo(valeurMax) <= 0)
                .count();

        // Calculer le ratio
        double ratio = (double) mesuresDansBornes / toutesMesures.size();
        return ratio * 100.0;  // Pourcentage
    }

    /**
     * Calcule le temps moyen entre incidents (alertes SEUIL_ABSOLU) sur une période.
     * Filtre correctement par point de mesure ET métrique via jointure Alerte → Mesure.
     */
    private Double calculerTempsMoyenEntreIncidents(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Alerte> alertes = alerteRepository.findByPointMesureAndMetriqueAndTypeAlerteAndPeriode(
                idPointMesure, metrique, TypeAlerte.SEUIL_ABSOLU, dateDebut, dateFin);

        if (alertes.size() < 2) {
            return null;
        }

        // Alertes déjà triées par createdAt ASC dans la requête
        long totalEcartHeures = 0;
        for (int i = 1; i < alertes.size(); i++) {
            LocalDateTime prev = alertes.get(i - 1).getCreatedAt();
            LocalDateTime curr = alertes.get(i).getCreatedAt();
            totalEcartHeures += Duration.between(prev, curr).toHours();
        }

        return (double) totalEcartHeures / (alertes.size() - 1);
    }

    /**
     * Calcule le temps moyen de retour à la normale (alertes résolues) sur une période.
     * Filtre correctement par point de mesure ET métrique via jointure Alerte → Mesure.
     */
    private Double calculerTempsMoyenRetourNormal(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Alerte> alertesResolues = alerteRepository.findByPointMesureAndMetriqueAndStatutAndPeriode(
                idPointMesure, metrique, StatutAlerte.RESOLUE, dateDebut, dateFin);

        if (alertesResolues.isEmpty()) {
            return null;
        }

        long totalDureeHeures = 0;
        int count = 0;
        for (Alerte alerte : alertesResolues) {
            if (alerte.getUpdatedAt() != null) {
                totalDureeHeures += Duration.between(alerte.getCreatedAt(), alerte.getUpdatedAt()).toHours();
                count++;
            }
        }

        return count > 0 ? (double) totalDureeHeures / count : null;
    }
}
