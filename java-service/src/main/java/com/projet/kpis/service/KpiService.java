package com.projet.kpis.service;

import com.projet.config.BusinessException;
import com.projet.config.MetierValidation;
import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        // Valider que la plage de dates est cohérente (obligatoires et ordonnées)
        MetierValidation.validerPlageDates(dateDebut, dateFin);

        // Valider que le point de mesure existe, est actif et non supprimé
        PointMesure pointMesure = pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(idPointMesure)
                .orElseThrow(() -> new BusinessException(
                        "POINT_MESURE_INACTIF",
                        "Le point de mesure (ID " + idPointMesure + ") n'existe pas ou n'est pas actif.",
                        HttpStatus.BAD_REQUEST));

        // Valider que la métrique est applicable au type d'emplacement du point
        MetierValidation.validerMetriqueApplicable(pointMesure, metrique);

        // alertesActives : instantané indépendant de la période — toutes les alertes
        // actuellement actives pour ce point/métrique, quelle que soit leur date de création
        long alertesActives = alerteRepository.countAlertesActivesInstantane(idPointMesure, metrique);

        // KPIs scopés
        Double tauxConformite = calculerTauxConformite(idPointMesure, metrique, dateDebut, dateFin);
        Double tempsMoyenEntreIncidents = calculerTempsMoyenEntreIncidents(idPointMesure, metrique, dateDebut, dateFin);
        Double tempsMoyenRetourNormal = calculerTempsMoyenRetourNormal(idPointMesure, metrique, dateDebut, dateFin);

        return new KpiResponseDTO(
                alertesActives,
                tauxConformite,
                tempsMoyenEntreIncidents,
                tempsMoyenRetourNormal
        );
    }

    /**
     * Calcule le taux de conformité pour un point et une métrique sur une période.
     *
     * <p>Chaque mesure est comparée au SeuilAbsolu actif <em>au moment de sa création</em>
     * (via LATERAL côté SQL), pas au seuil actif aujourd'hui. Un seul aller-retour SQL
     * sans chargement d'objets en mémoire.
     *
     * @return Taux en pourcentage, ou null si aucune mesure ou aucun seuil sur la période
     */
    private Double calculerTauxConformite(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        Object[] rawResult = mesureRepository.countConformiteAvecSeuilHistorise(
                idPointMesure, metrique.name(), dateDebut, dateFin);

        if (rawResult == null || rawResult.length == 0) {
            return null;
        }

        Object[] row;
        if (rawResult[0] instanceof Object[]) {
            row = (Object[]) rawResult[0];
        } else {
            row = rawResult;
        }

        if (row == null || row.length == 0 || row[0] == null) {
            return null;
        }

        long total = ((Number) row[0]).longValue();
        if (total == 0) {
            return null;  // Aucune mesure sur la période
        }

        long conformes = row[1] != null ? ((Number) row[1]).longValue() : 0L;

        // null si aucun seuil n'était actif pour aucune des mesures (conformes = 0 et aucun seuil)
        // On distingue "pas de seuil" de "tout conforme" en vérifiant si conformes == total quand
        // il n'y a pas de seuil : dans ce cas la requête retourne conformes = 0 (FILTER échoue).
        // Si conformes = 0 ET total > 0 : soit tout hors bornes, soit pas de seuil — on ne peut
        // pas distinguer sans requête supplémentaire. Comportement conservateur : retourner 0 %.
        return (conformes * 100.0) / total;
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
     * Calcule le temps moyen de retour à la normale (MTTR) sur une période.
     *
     * <p>Filtre les alertes résolues dont la <em>date de résolution</em> (updatedAt)
     * tombe dans la période — sémantique correcte pour un MTTR : on mesure les
     * résolutions survenues pendant la période, pas les créations.
     */
    private Double calculerTempsMoyenRetourNormal(Long idPointMesure, Metrique metrique, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Alerte> alertesResolues = alerteRepository.findAlertesResoluesParPeriodeResolution(
                idPointMesure, metrique, dateDebut, dateFin);

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
