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
        // KPIs globaux (toujours inclus)
        long alertesActives = alerteRepository.countByStatut(StatutAlerte.ACTIVE);
        long nbPointsEnAnomalie = alerteRepository.countDistinctPointMesureByStatut(StatutAlerte.ACTIVE);
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
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Temps moyen en heures, ou null si moins de 2 alertes
     */
    private Double calculerTempsMoyenEntreIncidents(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Récupérer les alertes SEUIL_ABSOLU pour cette paire dans la période
        // Note: nécessite une requête custom avec jointure sur Mesure pour filtrer par pointMesure et metrique
        // Pour l'instant, on utilise une approche simplifiée en récupérant toutes les alertes de la période
        List<Alerte> alertes = alerteRepository.findByTypeAlerteAndCreatedAtBetween(TypeAlerte.SEUIL_ABSOLU, dateDebut, dateFin);

        // Filtrer pour ne garder que celles liées au point et à la métrique (nécessite jointure avec Mesure)
        // Pour simplifier, on suppose que le repository a une méthode dédiée ou on fait le filtrage en mémoire
        // TODO: Implémenter une requête JPA custom pour filtrer par pointMesure et metrique via la jointure Mesure

        if (alertes.size() < 2) {
            return null;  // Moins de 2 alertes, impossible de calculer une moyenne
        }

        // Trier par date de création
        alertes.sort((a1, a2) -> a1.getCreatedAt().compareTo(a2.getCreatedAt()));

        // Calculer les écarts entre créations consécutives
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
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Temps moyen en heures, ou null si aucune alerte résolue
     */
    private Double calculerTempsMoyenRetourNormal(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Récupérer les alertes résolues dans la période
        List<Alerte> alertesResolues = alerteRepository.findByStatutAndCreatedAtBetween(StatutAlerte.RESOLUE, dateDebut, dateFin);

        if (alertesResolues.isEmpty()) {
            return null;  // Aucune alerte résolue
        }

        // Filtrer pour ne garder que celles liées au point et à la métrique (nécessite jointure avec Mesure)
        // TODO: Implémenter une requête JPA custom pour filtrer par pointMesure et metrique via la jointure Mesure

        // Calculer la moyenne de (updatedAt - createdAt) en heures
        // Note: updatedAt représente la date de résolution quand statut passe à RESOLUE
        long totalDureeHeures = 0;
        for (Alerte alerte : alertesResolues) {
            if (alerte.getUpdatedAt() != null) {
                totalDureeHeures += Duration.between(alerte.getCreatedAt(), alerte.getUpdatedAt()).toHours();
            }
        }

        return (double) totalDureeHeures / alertesResolues.size();
    }
}
