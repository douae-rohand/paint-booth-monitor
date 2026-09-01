package com.projet.reports.service;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.auth.model.Superviseur;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.kpis.service.KpiService;
import com.projet.measures.dto.MesureHistoriqueDTO;
import com.projet.measures.model.PointMesure;
import com.projet.measures.model.enums.Granularite;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import com.projet.measures.service.MesureHistoriqueService;
import com.projet.reports.model.RapportPDF;
import com.projet.reports.model.enums.StatutGeneration;
import com.projet.reports.model.enums.TypeRapport;
import com.projet.reports.pdf.RapportPdfBuilder;
import com.projet.reports.repository.RapportPdfRepository;
import com.projet.reports.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service d'orchestration pour la génération de rapports PDF.
 * Module: reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportGenerationService {

    private final RapportPdfRepository rapportPdfRepository;
    private final PointMesureRepository pointMesureRepository;
    private final MesureRepository mesureRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;
    private final AlerteRepository alerteRepository;
    private final KpiService kpiService;
    private final MesureHistoriqueService mesureHistoriqueService;
    private final MinioStorageService minioStorageService;

    /**
     * Génère un rapport PDF pour un point de mesure sur une période.
     *
     * @param idPointMesure ID du point de mesure
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param typeRapport Type de rapport (informatif)
     * @param superviseur Superviseur demandeur
     * @return RapportPDF généré
     */
    @Transactional
    public RapportPDF genererRapport(
            Long idPointMesure,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            TypeRapport typeRapport,
            Superviseur superviseur) {

        log.info("Génération rapport: idPointMesure={}, dateDebut={}, dateFin={}, typeRapport={}, superviseur={}",
                idPointMesure, dateDebut, dateFin, typeRapport, superviseur.getEmail());

        // Validation du point de mesure
        PointMesure pointMesure = pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(idPointMesure)
                .orElseThrow(() -> new IllegalArgumentException("Point de mesure non trouvé ou inactif: " + idPointMesure));

        // Validation des dates
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires");
        }
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }

        // Créer l'entité RapportPDF avec statut EN_COURS
        RapportPDF rapport = new RapportPDF();
        rapport.setSuperviseur(superviseur);
        rapport.setPointMesure(pointMesure);
        rapport.setTypeRapport(typeRapport);
        rapport.setPeriodeDebut(dateDebut);
        rapport.setPeriodeFin(dateFin);
        rapport.setStatutGeneration(StatutGeneration.EN_COURS);
        rapport = rapportPdfRepository.save(rapport);

        try {
            // Déterminer les métriques associées au point
            List<Metrique> metriques = determinerMetriques(pointMesure);

            // Construire le contexte de génération
            RapportPdfBuilder.RapportContext context = new RapportPdfBuilder.RapportContext();
            context.setNomPointMesure(pointMesure.getNom());
            context.setDateDebut(dateDebut);
            context.setDateFin(dateFin);
            context.setDateGeneration(LocalDateTime.now());
            context.setNomDemandeur(superviseur.getPrenom() + " " + superviseur.getNom());

            // Granularité déterminée une seule fois — transmise au builder pour le format de l'axe X
            Granularite granularite = determinerGranularite(dateDebut, dateFin);
            context.setGranularite(granularite);

            // Récupérer les données pour chaque métrique
            Map<Metrique, KpiResponseDTO> kpisByMetrique = new HashMap<>();
            Map<Metrique, List<MesureHistoriqueDTO>> historiqueByMetrique = new HashMap<>();
            Map<Metrique, SeuilAbsolu> seuilsAbsolu = new HashMap<>();
            Map<Metrique, RapportPdfBuilder.StatistiquesMetrique> statistiquesByMetrique = new HashMap<>();

            for (Metrique metrique : metriques) {
                // KPIs
                KpiResponseDTO kpi = kpiService.getKpisParPoint(idPointMesure, metrique, dateDebut, dateFin);
                kpisByMetrique.put(metrique, kpi);

                // Historique agrégé (réutilisation de la méthode du dashboard)
                List<MesureHistoriqueDTO> historique = mesureHistoriqueService.executerAggregation(
                        idPointMesure, metrique, dateDebut, dateFin, granularite);
                historiqueByMetrique.put(metrique, historique);

                // Seuil absolu actif
                Optional<SeuilAbsolu> seuil = seuilAbsoluRepository
                        .findByPointMesureIdAndMetriqueAndActifTrue(idPointMesure, metrique);
                seuil.ifPresent(value -> seuilsAbsolu.put(metrique, value));

                // Statistiques (min/max/moyenne)
                Object[] stats = mesureRepository.calculateStatisticsForPointAndPeriod(
                        idPointMesure, metrique.name(), dateDebut, dateFin);
                // La requête native retourne un Object[] dont chaque élément est lui-même un Object[]
                // (une ligne de résultat). On extrait la première ligne si elle existe.
                Object[] statRow = null;
                if (stats != null && stats.length > 0) {
                    if (stats[0] instanceof Object[]) {
                        statRow = (Object[]) stats[0];
                    } else {
                        // Certains drivers retournent directement la ligne aplatie
                        statRow = stats;
                    }
                }
                if (statRow != null && statRow[0] != null) {
                    RapportPdfBuilder.StatistiquesMetrique stat = new RapportPdfBuilder.StatistiquesMetrique();
                    stat.setMin(new BigDecimal(statRow[0].toString()));
                    stat.setMax(new BigDecimal(statRow[1].toString()));
                    stat.setMoyenne(new BigDecimal(statRow[2].toString()));
                    statistiquesByMetrique.put(metrique, stat);
                }
            }

            context.setKpisByMetrique(kpisByMetrique);
            context.setHistoriqueByMetrique(historiqueByMetrique);
            context.setSeuilsAbsolu(seuilsAbsolu);
            context.setStatistiquesByMetrique(statistiquesByMetrique);

            // Récupérer les alertes (max 50)
            List<Alerte> alertes = alerteRepository.findAlertesNative(
                    null, null, null, idPointMesure, dateDebut, dateFin, 50, 0);

            // Récupérer le total des alertes
            long totalAlertes = alerteRepository.countAlertesNative(
                    null, null, null, idPointMesure, dateDebut, dateFin);

            // Récupérer la répartition par type
            List<Object[]> repartition = alerteRepository.countAlertesByTypeForPointAndPeriod(
                    idPointMesure, dateDebut, dateFin);
            Map<TypeAlerte, Long> repartitionMap = new HashMap<>();
            for (Object[] row : repartition) {
                TypeAlerte type = TypeAlerte.valueOf((String) row[0]);
                Long count = (Long) row[1];
                repartitionMap.put(type, count);
            }

            // Mapper les alertes pour le tableau
            List<RapportPdfBuilder.AlerteRow> alerteRows = new ArrayList<>();
            for (Alerte alerte : alertes) {
                RapportPdfBuilder.AlerteRow row = new RapportPdfBuilder.AlerteRow();
                row.setMetrique(alerte.getMetrique() != null ? alerte.getMetrique().name() : "N/A");
                row.setTypeAlerte(alerte.getTypeAlerte() != null ? alerte.getTypeAlerte().name() : "N/A");
                row.setSeverite(alerte.getSeverite() != null ? alerte.getSeverite().name() : "N/A");
                row.setDateCreation(alerte.getCreatedAt());
                row.setStatut(alerte.getStatut() != null ? alerte.getStatut().name() : "N/A");
                alerteRows.add(row);
            }

            context.setAlertes(alerteRows);
            context.setTotalAlertes(totalAlertes);
            context.setRepartitionAlertes(repartitionMap);

            // Générer le PDF
            RapportPdfBuilder builder = new RapportPdfBuilder();
            byte[] pdfContent = builder.build(context);

            // Upload sur MinIO
            String objetKey = minioStorageService.generateObjetKey(rapport.getIdRapport(), LocalDateTime.now());
            String nomFichier = minioStorageService.generateNomFichier(
                    pointMesure.getNom(), dateDebut, dateFin);

            minioStorageService.upload(objetKey, pdfContent);

            // Mettre à jour le rapport
            rapport.setObjetMinioStorageKey(objetKey);
            rapport.setNomFichier(nomFichier);
            rapport.setTailleFichier((long) pdfContent.length);
            rapport.setStatutGeneration(StatutGeneration.TERMINE);
            rapport.setGeneratedAt(LocalDateTime.now());
            rapport = rapportPdfRepository.save(rapport);

            log.info("Rapport généré avec succès: idRapport={}, taille={} octets", rapport.getIdRapport(), pdfContent.length);

            return rapport;

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport: idRapport={}", rapport.getIdRapport(), e);
            rapport.setStatutGeneration(StatutGeneration.ECHEC);
            rapportPdfRepository.save(rapport);
            throw new RuntimeException("Erreur lors de la génération du rapport: " + e.getMessage(), e);
        }
    }

    /**
     * Détermine les métriques applicables à un point de mesure.
     * CABINE → TEMPERATURE + HUMIDITE
     * ETUVE → TEMPERATURE
     */
    private List<Metrique> determinerMetriques(PointMesure pointMesure) {
        if ("CABINE".equalsIgnoreCase(pointMesure.getTypeEmplacement())) {
            return List.of(Metrique.TEMPERATURE, Metrique.HUMIDITE);
        } else if ("ETUVE".equalsIgnoreCase(pointMesure.getTypeEmplacement())) {
            return List.of(Metrique.TEMPERATURE);
        } else {
            throw new IllegalArgumentException("Type d'emplacement non supporté: " + pointMesure.getTypeEmplacement());
        }
    }

    /**
     * Détermine la granularité d'agrégation selon la durée de la période.
     * Réutilisation de la logique de MesureHistoriqueService.determinerGranularite.
     */
    private Granularite determinerGranularite(LocalDateTime dateDebut, LocalDateTime dateFin) {
        long jours = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
        if (jours <= 1) {
            return Granularite.TRENTE_MIN;
        } else if (jours <= 7) {
            return Granularite.HORAIRE;
        } else if (jours <= 31) {
            return Granularite.JOURNALIERE;
        } else {
            return Granularite.MENSUELLE;
        }
    }
}
