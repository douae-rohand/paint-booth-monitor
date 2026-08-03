package com.projet.measures.service;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.measures.dto.MesureCabineDTO;
import com.projet.measures.dto.MesureEtuveDTO;
import com.projet.measures.dto.MesureHistoriqueDTO;
import com.projet.measures.dto.MesureHistoriqueResponseDTO;
import com.projet.measures.exception.MetriqueNonApplicableAuPointException;
import com.projet.measures.model.PointMesure;
import com.projet.measures.model.enums.Granularite;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service pour l'historique des mesures avec agrégation par granularité.
 * Module: measures
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesureHistoriqueService {

    private final MesureRepository mesureRepository;
    private final PointMesureRepository pointMesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;
    private final EntityManager entityManager;

    /**
     * Récupère l'historique des mesures pour un point de mesure et une métrique sur une période.
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique demandée
     * @param periode Période prédéfinie (24h, 7j, 30j, 6mois, 1an, personnalise)
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param granulariteDemandee Granularité demandée (optionnel, uniquement utilisé pour periode=7j)
     * @return MesureHistoriqueResponseDTO avec les points agrégés et le seuil absolu actif
     * @throws MetriqueNonApplicableAuPointException si la métrique n'est pas applicable au point
     */
    public MesureHistoriqueResponseDTO getHistorique(
            Long idPointMesure,
            Metrique metrique,
            String periode,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Granularite granulariteDemandee) {

        // Valider que le PointMesure existe
        PointMesure pointMesure = pointMesureRepository.findById(idPointMesure)
                .orElseThrow(() -> new IllegalArgumentException("Point de mesure non trouvé avec ID: " + idPointMesure));

        // Valider que la métrique est applicable au point
        validerMetriqueApplicable(pointMesure, metrique);

        // Déterminer la granularité à appliquer
        Granularite granulariteAppliquee = determinerGranularite(periode, dateDebut, dateFin, granulariteDemandee);

        // Exécuter la requête d'agrégation selon la granularité
        List<MesureHistoriqueDTO> points = executerAggregation(
                idPointMesure, metrique, dateDebut, dateFin, granulariteAppliquee);

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

        return new MesureHistoriqueResponseDTO(points, seuilAbsoluDTO, granulariteAppliquee);
    }

    /**
     * Détermine la granularité à appliquer selon la période et les règles métier.
     *
     * @param periode Période prédéfinie
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @param granulariteDemandee Granularité demandée (optionnel)
     * @return Granularité à appliquer
     */
    private Granularite determinerGranularite(
            String periode,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Granularite granulariteDemandee) {

        switch (periode) {
            case "24h":
                return Granularite.TRENTE_MIN;

            case "7j":
                // Utiliser la granularité demandée si fournie, sinon HORAIRE par défaut
                if (granulariteDemandee != null) {
                    if (granulariteDemandee == Granularite.HORAIRE || granulariteDemandee == Granularite.JOURNALIERE) {
                        return granulariteDemandee;
                    }
                    log.warn("Granularité demandée invalide pour période 7j: {}, utilisation de HORAIRE par défaut", granulariteDemandee);
                }
                return Granularite.HORAIRE;

            case "30j":
                return Granularite.JOURNALIERE;

            case "6mois":
            case "1an":
                return Granularite.MENSUELLE;

            case "personnalise":
                // Déduire automatiquement selon l'écart
                long jours = ChronoUnit.DAYS.between(dateDebut, dateFin);
                if (jours <= 1) {
                    return Granularite.TRENTE_MIN;
                } else if (jours <= 7) {
                    return Granularite.HORAIRE;
                } else if (jours <= 31) {
                    return Granularite.JOURNALIERE;
                } else {
                    return Granularite.MENSUELLE;
                }

            default:
                log.warn("Période inconnue: {}, utilisation de JOURNALIERE par défaut", periode);
                return Granularite.JOURNALIERE;
        }
    }

    /**
     * Exécute la requête d'agrégation selon la granularité.
     *
     * @param idPointMesure ID du point de mesure
     * @param metrique Métrique
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @param granularite Granularité à appliquer
     * @return Liste des points agrégés
     */
    @SuppressWarnings("unchecked")
    private List<MesureHistoriqueDTO> executerAggregation(
            Long idPointMesure,
            Metrique metrique,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Granularite granularite) {

        String sql;
        switch (granularite) {
            case TRENTE_MIN:
                sql = """
                    SELECT
                      date_trunc('hour', created_at) + (INTERVAL '30 min' * FLOOR(EXTRACT(MINUTE FROM created_at) / 30)) AS bucket,
                      AVG(valeur) AS valeur_moy
                    FROM mesure
                    WHERE id_point_mesure = :idPointMesure
                      AND metrique = :metrique
                      AND plausible = true
                      AND created_at BETWEEN :dateDebut AND :dateFin
                    GROUP BY bucket
                    ORDER BY bucket
                    """;
                break;

            case HORAIRE:
                sql = """
                    SELECT date_trunc('hour', created_at) AS bucket, AVG(valeur) AS valeur_moy
                    FROM mesure
                    WHERE id_point_mesure = :idPointMesure
                      AND metrique = :metrique
                      AND plausible = true
                      AND created_at BETWEEN :dateDebut AND :dateFin
                    GROUP BY bucket
                    ORDER BY bucket
                    """;
                break;

            case JOURNALIERE:
                sql = """
                    SELECT date_trunc('day', created_at) AS bucket, AVG(valeur) AS valeur_moy
                    FROM mesure
                    WHERE id_point_mesure = :idPointMesure
                      AND metrique = :metrique
                      AND plausible = true
                      AND created_at BETWEEN :dateDebut AND :dateFin
                    GROUP BY bucket
                    ORDER BY bucket
                    """;
                break;

            case MENSUELLE:
                sql = """
                    SELECT date_trunc('month', created_at) AS bucket, AVG(valeur) AS valeur_moy
                    FROM mesure
                    WHERE id_point_mesure = :idPointMesure
                      AND metrique = :metrique
                      AND plausible = true
                      AND created_at BETWEEN :dateDebut AND :dateFin
                    GROUP BY bucket
                    ORDER BY bucket
                    """;
                break;

            default:
                throw new IllegalArgumentException("Granularité non supportée: " + granularite);
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("idPointMesure", idPointMesure);
        query.setParameter("metrique", metrique.name());
        query.setParameter("dateDebut", dateDebut);
        query.setParameter("dateFin", dateFin);

        List<Object[]> results = query.getResultList();
        List<MesureHistoriqueDTO> points = new ArrayList<>();

        for (Object[] row : results) {
            LocalDateTime bucket = (LocalDateTime) row[0];
            BigDecimal valeurMoy = (BigDecimal) row[1];
            points.add(new MesureHistoriqueDTO(bucket, valeurMoy));
        }

        return points;
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

    /**
     * Récupère l'historique des mesures de la cabine avec pivot température/humidité par cycle.
     *
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @param pageable Pagination
     * @return Page de MesureCabineDTO
     */
    public Page<MesureCabineDTO> getHistoriqueCabine(
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            boolean seulementDepassements,
            Pageable pageable) {

        // Résoudre l'ID du PointMesure cabine (type_emplacement = 'CABINE', actif = true)
        List<PointMesure> cabines = pointMesureRepository.findByTypeEmplacement("CABINE");
        if (cabines.isEmpty()) {
            log.warn("Aucun point de mesure de type CABINE trouvé");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Prendre le premier point de mesure cabine actif
        PointMesure cabine = cabines.stream()
                .filter(PointMesure::isActif)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun point de mesure cabine actif trouvé"));

        log.info("Récupération historique cabine: idPointMesure={}, dateDebut={}, dateFin={}, seulementDepassements={}",
                cabine.getId(), dateDebut, dateFin, seulementDepassements);

        // Exécuter la requête de comptage
        long total = mesureRepository.countHistoriqueCabine(
                cabine.getId(), dateDebut, dateFin, seulementDepassements);

        // Exécuter la requête de données
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        List<Object[]> results = mesureRepository.findHistoriqueCabine(
                cabine.getId(), dateDebut, dateFin, seulementDepassements, limit, offset);

        // Mapper les résultats vers MesureCabineDTO
        List<MesureCabineDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            LocalDateTime timestampCycle = (LocalDateTime) row[0];
            String caisseId = (String) row[1];
            BigDecimal temperature = row[2] != null ? (BigDecimal) row[2] : null;
            BigDecimal humidite = row[3] != null ? (BigDecimal) row[3] : null;
            boolean depassementTemperature = row[4] != null && ((Boolean) row[4]);
            boolean depassementHumidite = row[5] != null && ((Boolean) row[5]);

            dtos.add(new MesureCabineDTO(
                    timestampCycle,
                    caisseId,
                    temperature,
                    humidite,
                    depassementTemperature,
                    depassementHumidite
            ));
        }

        log.info("Historique cabine récupéré: {} enregistrements sur {} total", dtos.size(), total);
        return new PageImpl<>(dtos, pageable, total);
    }

    /**
     * Récupère l'historique des mesures de l'étuve par zone.
     *
     * @param zone Nom de la zone (optionnel, "ZONE_1".."ZONE_5" ou null pour toutes zones)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @param pageable Pagination
     * @return Page de MesureEtuveDTO
     */
    public Page<MesureEtuveDTO> getHistoriqueEtuve(
            String zone,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            boolean seulementDepassements,
            Pageable pageable) {

        // Résoudre les IDs des points de mesure étuve
        List<Long> idsZones = null;
        if (zone != null && !zone.trim().isEmpty()) {
            // Zone spécifique demandée
            PointMesure pointMesure = pointMesureRepository.findByNom(zone)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Point de mesure non trouvé pour la zone: " + zone));

            if (!"ETUVE".equalsIgnoreCase(pointMesure.getTypeEmplacement())) {
                throw new IllegalArgumentException(
                        "Le point de mesure " + zone + " n'est pas de type ETUVE");
            }

            idsZones = List.of(pointMesure.getId());
            log.info("Récupération historique étuve zone spécifique: zone={}, idPointMesure={}", zone, pointMesure.getId());
        } else {
            // Toutes les zones étuve
            List<PointMesure> etuves = pointMesureRepository.findByTypeEmplacement("ETUVE");
            idsZones = etuves.stream()
                    .filter(PointMesure::isActif)
                    .map(PointMesure::getId)
                    .toList();

            if (idsZones.isEmpty()) {
                log.warn("Aucun point de mesure de type ETUVE actif trouvé");
                return new PageImpl<>(List.of(), pageable, 0);
            }

            log.info("Récupération historique étuve toutes zones: {} zones", idsZones.size());
        }

        log.info("Récupération historique étuve: dateDebut={}, dateFin={}, seulementDepassements={}",
                dateDebut, dateFin, seulementDepassements);

        // Exécuter la requête de comptage
        long total = mesureRepository.countHistoriqueEtuve(
                idsZones != null ? idsZones.toArray(new Long[0]) : null, dateDebut, dateFin, seulementDepassements);

        // Exécuter la requête de données
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        List<Object[]> results = mesureRepository.findHistoriqueEtuve(
                idsZones != null ? idsZones.toArray(new Long[0]) : null, dateDebut, dateFin, seulementDepassements, limit, offset);

        // Mapper les résultats vers MesureEtuveDTO
        List<MesureEtuveDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            UUID idMesure = (UUID) row[0];
            LocalDateTime dateMesure = (LocalDateTime) row[1];
            String zoneNom = (String) row[2];
            BigDecimal temperature = row[3] != null ? (BigDecimal) row[3] : null;
            boolean depassement = row[4] != null && ((Boolean) row[4]);

            dtos.add(new MesureEtuveDTO(
                    idMesure,
                    dateMesure,
                    zoneNom,
                    temperature,
                    depassement
            ));
        }

        log.info("Historique étuve récupéré: {} enregistrements sur {} total", dtos.size(), total);
        return new PageImpl<>(dtos, pageable, total);
    }
}
