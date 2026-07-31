package com.projet.alerting.service;

import com.projet.alerting.dto.DetailJourAlertesDTO;
import com.projet.alerting.dto.HeatmapJourDTO;
import com.projet.alerting.dto.TopAlerteDTO;
import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour les statistiques d'alertes.
 * Module: alerting
 */
@Service
@RequiredArgsConstructor
public class AlerteStatsService {

    private final AlerteRepository alerteRepository;
    private final PointMesureRepository pointMesureRepository;
    private final MesureRepository mesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;

    /**
     * Récupère les top métriques en alerte sur une période.
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @param idPointMesure ID du point de mesure (optionnel)
     * @param limit Nombre maximum de résultats (défaut 10)
     * @return Liste des top alertes
     */
    public List<TopAlerteDTO> getTopAlertes(LocalDateTime dateDebut, LocalDateTime dateFin, Long idPointMesure, int limit) {
        // Récupérer toutes les alertes de la période
        List<Alerte> alertes = alerteRepository.findByCreatedAtBetween(dateDebut, dateFin);

        // Filtrer par point de mesure si fourni
        if (idPointMesure != null) {
            alertes = alertes.stream()
                    .filter(a -> {
                        // Nécessite jointure avec Mesure pour filtrer par pointMesure
                        // Pour simplifier, on récupère la mesure liée
                        Mesure mesure = mesureRepository.findById(a.getIdMesure()).orElse(null);
                        return mesure != null && mesure.getPointMesure().getId().equals(idPointMesure);
                    })
                    .collect(Collectors.toList());
        }

        // Grouper par (id_point_mesure, metrique) et compter
        Map<String, Long> groupedCount = alertes.stream()
                .collect(Collectors.groupingBy(
                        a -> {
                            Mesure mesure = mesureRepository.findById(a.getIdMesure()).orElse(null);
                            if (mesure == null) return "unknown";
                            return mesure.getPointMesure().getId() + "_" + a.getMetrique();
                        },
                        Collectors.counting()
                ));

        // Convertir en DTOs et trier par count décroissant
        List<TopAlerteDTO> result = groupedCount.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    Long pointId = Long.parseLong(parts[0]);
                    Metrique metrique = Metrique.valueOf(parts[1]);

                    PointMesure point = pointMesureRepository.findById(pointId).orElse(null);
                    String nomPoint = point != null ? point.getNom() : "Inconnu";

                    return new TopAlerteDTO(pointId, nomPoint, metrique, entry.getValue());
                })
                .sorted((a, b) -> Long.compare(b.getNombreDepassements(), a.getNombreDepassements()))
                .limit(limit)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * Récupère les données de heatmap pour un mois donné.
     *
     * @param annee Année
     * @param mois Mois (1-12)
     * @param idPointMesure ID du point de mesure (optionnel)
     * @param metrique Métrique (optionnel)
     * @return Liste des données par jour du mois
     */
    public List<HeatmapJourDTO> getHeatmapMois(int annee, int mois, Long idPointMesure, Metrique metrique) {
        YearMonth yearMonth = YearMonth.of(annee, mois);
        LocalDateTime dateDebut = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime dateFin = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Récupérer les alertes du mois
        List<Alerte> alertes = alerteRepository.findByCreatedAtBetween(dateDebut, dateFin);

        // Filtrer par point et métrique si fournis
        if (idPointMesure != null || metrique != null) {
            alertes = alertes.stream()
                    .filter(a -> {
                        Mesure mesure = mesureRepository.findById(a.getIdMesure()).orElse(null);
                        if (mesure == null) return false;

                        if (idPointMesure != null && !mesure.getPointMesure().getId().equals(idPointMesure)) {
                            return false;
                        }
                        if (metrique != null && !a.getMetrique().equals(metrique)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Grouper par jour du mois
        Map<Integer, Long> countByDay = alertes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().getDayOfMonth(),
                        Collectors.counting()
                ));

        // Créer la liste complète des jours du mois (même ceux à 0)
        List<HeatmapJourDTO> result = new ArrayList<>();
        int daysInMonth = yearMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            long count = countByDay.getOrDefault(day, 0L);
            result.add(new HeatmapJourDTO(day, LocalDate.of(annee, mois, day), count));
        }

        return result;
    }

    /**
     * Récupère le détail des alertes pour un jour donné.
     *
     * @param date Date du jour (format YYYY-MM-DD)
     * @param idPointMesure ID du point de mesure (optionnel)
     * @param metrique Métrique (optionnel)
     * @return Détail des alertes du jour
     */
    public DetailJourAlertesDTO getDetailJour(LocalDate date, Long idPointMesure, Metrique metrique) {
        LocalDateTime dateDebut = date.atStartOfDay();
        LocalDateTime dateFin = date.atTime(23, 59, 59);

        // Récupérer les alertes SEUIL_ABSOLU du jour
        List<Alerte> alertes = alerteRepository.findByTypeAlerteAndCreatedAtBetween(TypeAlerte.SEUIL_ABSOLU, dateDebut, dateFin);

        // Filtrer par point de mesure et/ou métrique si fournis
        if (idPointMesure != null || metrique != null) {
            alertes = alertes.stream()
                    .filter(a -> {
                        Mesure mesure = mesureRepository.findById(a.getIdMesure()).orElse(null);
                        if (mesure == null) return false;
                        if (idPointMesure != null && !mesure.getPointMesure().getId().equals(idPointMesure)) {
                            return false;
                        }
                        if (metrique != null && !a.getMetrique().equals(metrique)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        long nombreTotal = alertes.size();

        // Grouper par (id_point_mesure, metrique) : compter et prendre MAX(valeur)
        Map<String, DetailJourAlertesDTO.DetailAlerteDTO> groupedDetails = new HashMap<>();

        for (Alerte alerte : alertes) {
            Mesure mesure = mesureRepository.findById(alerte.getIdMesure()).orElse(null);
            if (mesure == null) continue;

            String key = mesure.getPointMesure().getId() + "_" + alerte.getMetrique();

            DetailJourAlertesDTO.DetailAlerteDTO dto = groupedDetails.get(key);
            if (dto == null) {
                dto = new DetailJourAlertesDTO.DetailAlerteDTO();
                dto.setIdPointMesure(mesure.getPointMesure().getId());
                dto.setNomPointMesure(mesure.getPointMesure().getNom());
                dto.setMetrique(alerte.getMetrique());
                dto.setNombreDepassements(1L);
                dto.setValeurMaxAtteinte(mesure.getValeur());

                // Récupérer le seuil configuré (approximation : seuil actuellement actif)
                // Note: Dans une implémentation idéale, on récupérerait le seuil au moment de l'alerte
                // en comparant date_creation avec date_activation/date_desactivation du SeuilAbsolu
                SeuilAbsolu seuilAbsolu = seuilAbsoluRepository
                        .findByPointMesureIdAndMetriqueAndActifTrue(mesure.getPointMesure().getId(), alerte.getMetrique())
                        .orElse(null);

                if (seuilAbsolu != null) {
                    DetailJourAlertesDTO.SeuilConfigureDTO seuilDTO = new DetailJourAlertesDTO.SeuilConfigureDTO();
                    seuilDTO.setValeurMin(seuilAbsolu.getValeurMin());
                    seuilDTO.setValeurMax(seuilAbsolu.getValeurMax());
                    dto.setSeuilConfigure(seuilDTO);
                }

                groupedDetails.put(key, dto);
            } else {
                // Incrémenter le compteur
                dto.setNombreDepassements(dto.getNombreDepassements() + 1);

                // Mettre à jour la valeur max si nécessaire
                if (mesure.getValeur().compareTo(dto.getValeurMaxAtteinte()) > 0) {
                    dto.setValeurMaxAtteinte(mesure.getValeur());
                }
            }
        }

        List<DetailJourAlertesDTO.DetailAlerteDTO> details = new ArrayList<>(groupedDetails.values());

        return new DetailJourAlertesDTO(date, nombreTotal, details);
    }
}
