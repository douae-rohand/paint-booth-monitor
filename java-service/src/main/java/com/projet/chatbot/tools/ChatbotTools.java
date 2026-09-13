package com.projet.chatbot.tools;

import com.projet.alerting.model.Alerte;
import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.SeuilDynamique;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.repository.AlerteRepository;
import com.projet.alerting.repository.SeuilAbsoluRepository;
import com.projet.alerting.repository.SeuilDynamiqueRepository;
import com.projet.config.BusinessException;
import com.projet.config.MetierValidation;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.kpis.service.KpiService;
import com.projet.measures.dto.MesureHistoriqueDTO;
import com.projet.measures.dto.PointMesureStatutDTO;
import com.projet.measures.model.Mesure;
import com.projet.measures.model.PointMesure;
import com.projet.config.model.enums.Granularite;
import com.projet.config.GranulariteUtils;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import com.projet.measures.service.MesureHistoriqueService;
import com.projet.measures.service.StatutTempsReelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Registre des 9 outils exposés au LLM via Spring AI (@Tool).
 * Module: chatbot
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotTools {

    private final PointMesureRepository pointMesureRepository;
    private final MesureRepository mesureRepository;
    private final AlerteRepository alerteRepository;
    private final SeuilAbsoluRepository seuilAbsoluRepository;
    private final SeuilDynamiqueRepository seuilDynamiqueRepository;
    private final StatutTempsReelService statutTempsReelService;
    private final MesureHistoriqueService mesureHistoriqueService;
    private final KpiService kpiService;
    private final ResoudrePeriodeTool resoudrePeriodeTool;

    // ── Records DTOs pour le LLM ──────────────────────────────────────────────

    public record PointMesureInfoDTO(
            Long id,
            String nom,
            String typeEmplacement,
            List<String> metriquesApplicables
    ) {}

    public record MesureActuelleDTO(
            String metrique,
            BigDecimal valeur,
            String horodatage
    ) {}

    public record ResumeEtatGlobalDTO(
            long totalPointsActifs,
            long nbNominal,
            long nbAttention,
            long nbCritique,
            long nbInconnu
    ) {}

    public record AlerteInfoDTO(
            String idAlerte,
            Long idPointMesure,
            String metrique,
            String typeAlerte,
            String severite,
            String statut,
            String dateCreation
    ) {}

    public record SeuilsActifsDTO(
            Long idPointMesure,
            String metrique,
            SeuilAbsoluDTO seuilAbsolu,
            SeuilDynamiqueDTO seuilDynamique
    ) {
        public record SeuilAbsoluDTO(BigDecimal valeurMin, BigDecimal valeurMax) {}
        public record SeuilDynamiqueDTO(BigDecimal minCalcule, BigDecimal maxCalcule, BigDecimal margeConfiguree, String dateCalcul) {}
    }

    // ── Outil 1 : getListePointsMesure ───────────────────────────────────────

    @Tool(name = "getListePointsMesure", description = "Retourne la liste de tous les points de mesure actifs (ID, nom, typeEmplacement, métriques applicables). À utiliser pour retrouver l'ID d'un point lorsqu'un utilisateur mentionne 'cabine', 'zone 1', etc.")
    public List<PointMesureInfoDTO> getListePointsMesure() {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getListePointsMesure (Timestamp: {} ms)", tStart);
        List<PointMesure> points = pointMesureRepository.findAllByActifTrue();
        List<PointMesureInfoDTO> result = points.stream().map(p -> new PointMesureInfoDTO(
                p.getId(),
                p.getNom(),
                p.getTypeEmplacement(),
                MetierValidation.metriquesApplicables(p.getTypeEmplacement()).stream().map(Enum::name).toList()
        )).toList();
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getListePointsMesure (Durée: {} ms, Nb points: {})", dur, result.size());
        return result;
    }

    // ── Outil 2 : getMesureActuelle ──────────────────────────────────────────

    @Tool(name = "getMesureActuelle", description = "Récupère les dernières valeurs mesurées pour un point de mesure spécifique (pour toutes ses métriques applicables).")
    public List<MesureActuelleDTO> getMesureActuelle(
            @ToolParam(description = "ID numérique du point de mesure (ex: 1, 2). Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getMesureActuelle (idPoint={}, Timestamp: {} ms)", idPointMesure, tStart);
        PointMesure point = validerEtChargerPoint(idPointMesure);
        Long pointId = point.getId();
        List<Metrique> metriques = MetierValidation.metriquesApplicables(point.getTypeEmplacement());

        List<MesureActuelleDTO> result = new ArrayList<>();
        for (Metrique m : metriques) {
            List<Mesure> dernieres = mesureRepository.findTopByIdPointMesureAndMetriqueAndPlausibleTrueOrderByCreatedAtDesc(pointId, m);
            if (!dernieres.isEmpty()) {
                Mesure mes = dernieres.get(0);
                result.add(new MesureActuelleDTO(m.name(), mes.getValeur(), mes.getCreatedAt().toString()));
            }
        }
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getMesureActuelle (idPoint={}, Durée: {} ms)", pointId, dur);
        return result;
    }

    // ── Outil 3 : getStatutTempsReel ─────────────────────────────────────────

    @Tool(name = "getStatutTempsReel", description = "Récupère le statut temps réel (CRITIQUE, ATTENTION, NOMINAL, INCONNU) et les dernières valeurs de mesure pour un point de mesure.")
    public PointMesureStatutDTO getStatutTempsReel(
            @ToolParam(description = "ID numérique du point de mesure (ex: 1, 2). Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getStatutTempsReel (idPoint={}, Timestamp: {} ms)", idPointMesure, tStart);
        PointMesure point = validerEtChargerPoint(idPointMesure);

        List<PointMesureStatutDTO> tous = statutTempsReelService.getStatutTousPoints();
        PointMesureStatutDTO result = tous.stream()
                .filter(p -> Objects.equals(p.getIdPointMesure(), point.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("STATUT_NON_TROUVE", "Impossible de récupérer le statut du point.", HttpStatus.NOT_FOUND));
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getStatutTempsReel (idPoint={}, Durée: {} ms)", point.getId(), dur);
        return result;
    }

    // ── Outil 4 : getResumeEtatGlobal ────────────────────────────────────────

    @Tool(name = "getResumeEtatGlobal", description = "Retourne la vue d'ensemble du système : nombre total de points actifs et répartition du nombre de points par statut (CRITIQUE, ATTENTION, NOMINAL, INCONNU).")
    public ResumeEtatGlobalDTO getResumeEtatGlobal() {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getResumeEtatGlobal (Timestamp: {} ms)", tStart);
        List<PointMesureStatutDTO> tous = statutTempsReelService.getStatutTousPoints();

        long total = tous.size();
        long nominal = 0;
        long attention = 0;
        long critique = 0;
        long inconnu = 0;

        for (PointMesureStatutDTO point : tous) {
            boolean aCritique = point.getMesures().stream().anyMatch(m -> m.getStatut() == PointMesureStatutDTO.StatutMesure.CRITIQUE);
            boolean aAttention = point.getMesures().stream().anyMatch(m -> m.getStatut() == PointMesureStatutDTO.StatutMesure.ATTENTION);
            boolean aInconnu = point.getMesures().stream().allMatch(m -> m.getStatut() == PointMesureStatutDTO.StatutMesure.INCONNU);

            if (aCritique) critique++;
            else if (aAttention) attention++;
            else if (aInconnu) inconnu++;
            else nominal++;
        }

        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getResumeEtatGlobal (Durée: {} ms, Total points: {})", dur, total);
        return new ResumeEtatGlobalDTO(total, nominal, attention, critique, inconnu);
    }

    // ── Outil 5 : getAlertesActives ──────────────────────────────────────────

    @Tool(name = "getAlertesActives", description = "Récupère les alertes actuellement actives (statut ACTIVE) non supprimées, filtrables par point de mesure.")
    public List<AlerteInfoDTO> getAlertesActives(
            @ToolParam(required = false, description = "ID numérique du point de mesure (ex: 1, 2). OMETTRE COMPLÈTEMENT ce paramètre s'il n'est pas spécifié par l'utilisateur. Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure) {
        long tStart = System.currentTimeMillis();
        Long pointId = parseOptionalLong(idPointMesure);
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getAlertesActives (idPointBrut={}, idPointParsed={}, Timestamp: {} ms)", idPointMesure, pointId, tStart);
        if (pointId != null) {
            validerEtChargerPoint(pointId);
        }

        List<Alerte> alertesActives = alerteRepository.findAlertesActives();
        List<AlerteInfoDTO> result = alertesActives.stream()
                .filter(a -> pointId == null || estAlerteSurPoint(a, pointId))
                .map(a -> new AlerteInfoDTO(
                        a.getIdAlerte().toString(),
                        pointId,
                        a.getMetrique() != null ? a.getMetrique().name() : null,
                        a.getTypeAlerte() != null ? a.getTypeAlerte().name() : null,
                        a.getSeverite() != null ? a.getSeverite().name() : null,
                        a.getStatut() != null ? a.getStatut().name() : null,
                        a.getCreatedAt().toString()
                )).toList();
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getAlertesActives (idPoint={}, Durée: {} ms, Nb alertes: {})", pointId, dur, result.size());
        return result;
    }

    // ── Outil 6 : getHistoriqueMesures ───────────────────────────────────────

    @Tool(name = "getHistoriqueMesures", description = "Récupère l'historique agrégé des mesures pour un point et une métrique sur une période donnée (format ISO-8601 YYYY-MM-DDTHH:mm:ss).")
    public List<MesureHistoriqueDTO> getHistoriqueMesures(
            @ToolParam(description = "ID numérique du point de mesure (ex: 1, 2). Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure,
            @ToolParam(description = "Métrique ('TEMPERATURE' ou 'HUMIDITE')") String metrique,
            @ToolParam(description = "Date/heure de début (format ISO-8601, ex: 2026-09-01T00:00:00)") String dateDebut,
            @ToolParam(description = "Date/heure de fin (format ISO-8601, ex: 2026-09-02T23:59:59)") String dateFin) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getHistoriqueMesures (idPoint={}, metrique={}, debut={}, fin={}, Timestamp: {} ms)", idPointMesure, metrique, dateDebut, dateFin, tStart);

        PointMesure point = validerEtChargerPoint(idPointMesure);
        Long pointId = point.getId();
        Metrique metriqueEnum = validerEtParseMetrique(metrique);
        MetierValidation.validerMetriqueApplicable(point, metriqueEnum);

        LocalDateTime start = parseDateISO(dateDebut, "dateDebut");
        LocalDateTime end = parseDateISO(dateFin, "dateFin");
        MetierValidation.validerPlageDates(start, end);
        MetierValidation.validerDureeMaximale(start, end);

        Granularite gran = GranulariteUtils.determinerDepuisEcart(start, end);
        List<MesureHistoriqueDTO> result = mesureHistoriqueService.executerAggregation(pointId, metriqueEnum, start, end, gran);
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getHistoriqueMesures (idPoint={}, metrique={}, Durée: {} ms, Nb points hist: {})", pointId, metrique, dur, result.size());
        return result;
    }

    // ── Outil 7 : getKpis ───────────────────────────────────────────────────

    @Tool(name = "getKpis", description = "Récupère les KPIs (alertesActives en instantané, tauxConformite en %, tempsMoyenEntreIncidents en heures, tempsMoyenRetourNormal en heures) pour un point et une métrique sur une période (ISO-8601).")
    public KpiResponseDTO getKpis(
            @ToolParam(description = "ID numérique du point de mesure (ex: 1, 2). Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure,
            @ToolParam(description = "Métrique ('TEMPERATURE' ou 'HUMIDITE')") String metrique,
            @ToolParam(description = "Date/heure de début (format ISO-8601)") String dateDebut,
            @ToolParam(description = "Date/heure de fin (format ISO-8601)") String dateFin) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getKpis (idPoint={}, metrique={}, debut={}, fin={}, Timestamp: {} ms)", idPointMesure, metrique, dateDebut, dateFin, tStart);

        PointMesure point = validerEtChargerPoint(idPointMesure);
        Long pointId = point.getId();
        Metrique metriqueEnum = validerEtParseMetrique(metrique);
        MetierValidation.validerMetriqueApplicable(point, metriqueEnum);

        LocalDateTime start = parseDateISO(dateDebut, "dateDebut");
        LocalDateTime end = parseDateISO(dateFin, "dateFin");
        MetierValidation.validerPlageDates(start, end);
        MetierValidation.validerDureeMaximale(start, end);

        KpiResponseDTO result = kpiService.getKpisParPoint(pointId, metriqueEnum, start, end);
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getKpis (idPoint={}, metrique={}, Durée: {} ms)", pointId, metrique, dur);
        return result;
    }

    // ── Outil 8 : getSeuilsActifs ────────────────────────────────────────────

    @Tool(name = "getSeuilsActifs", description = "Récupère les seuils configurés actuellement actifs (SeuilAbsolu min/max et SeuilDynamique min/max calculés) pour un point de mesure et une métrique.")
    public SeuilsActifsDTO getSeuilsActifs(
            @ToolParam(description = "ID numérique du point de mesure (ex: 1, 2). Ne jamais envoyer 'None', 'null' ou 0.") Object idPointMesure,
            @ToolParam(description = "Métrique ('TEMPERATURE' ou 'HUMIDITE')") String metrique) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: getSeuilsActifs (idPoint={}, metrique={}, Timestamp: {} ms)", idPointMesure, metrique, tStart);

        PointMesure point = validerEtChargerPoint(idPointMesure);
        Long pointId = point.getId();
        Metrique metriqueEnum = validerEtParseMetrique(metrique);
        MetierValidation.validerMetriqueApplicable(point, metriqueEnum);

        Optional<SeuilAbsolu> sa = seuilAbsoluRepository.findByPointMesureIdAndMetriqueAndActifTrue(pointId, metriqueEnum);
        Optional<SeuilDynamique> sd = seuilDynamiqueRepository.findByPointMesureIdAndMetriqueAndDeletedAtIsNull(pointId, metriqueEnum);

        SeuilsActifsDTO.SeuilAbsoluDTO saDTO = sa.map(s -> new SeuilsActifsDTO.SeuilAbsoluDTO(s.getValeurMin(), s.getValeurMax())).orElse(null);
        SeuilsActifsDTO.SeuilDynamiqueDTO sdDTO = sd.map(s -> new SeuilsActifsDTO.SeuilDynamiqueDTO(
                s.getValeurMinCalculee(), s.getValeurMaxCalculee(), s.getMargeConfiguree(), s.getDateCalcul() != null ? s.getDateCalcul().toString() : null
        )).orElse(null);

        SeuilsActifsDTO result = new SeuilsActifsDTO(pointId, metriqueEnum.name(), saDTO, sdDTO);
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: getSeuilsActifs (idPoint={}, metrique={}, Durée: {} ms)", pointId, dur);
        return result;
    }

    // ── Outil 9 : resoudre_periode ───────────────────────────────────────────

    @Tool(name = "resoudre_periode", description = "Convertit une expression temporelle relative ('hier', 'cette semaine', 'la semaine dernière', 'ce mois-ci', 'les 7 derniers jours', 'aujourd'hui') en dates ISO-8601 (dateDebut et dateFin). À appeler impérativement pour toute question avec date relative.")
    public ResoudrePeriodeTool.PeriodeResult resoudrePeriode(
            @ToolParam(description = "Expression temporelle relative à résoudre") String expression) {
        long tStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC TOOL] >>> DEBUT outil: resoudre_periode (expr='{}', Timestamp: {} ms)", expression, tStart);
        ResoudrePeriodeTool.PeriodeResult result = resoudrePeriodeTool.resoudre(expression);
        long dur = System.currentTimeMillis() - tStart;
        log.info("[DIAGNOSTIC TOOL] <<< FIN outil: resoudre_periode (expr='{}', Durée: {} ms, Résultat: {} -> {})", expression, dur, result.dateDebut(), result.dateFin());
        return result;
    }

    // ── Méthodes utilitaires privées ─────────────────────────────────────────

    private Long parseOptionalLong(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String str = String.valueOf(rawValue).trim();
        if (str.isEmpty() || str.equalsIgnoreCase("none") || str.equalsIgnoreCase("null")
                || str.equalsIgnoreCase("n/a") || str.equalsIgnoreCase("undefined")
                || str.equals("0")) {
            return null;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            log.warn("[ChatbotTools] Valeur non numérique reçue pour ID point de mesure optionnel: '{}', traitée comme nulle.", rawValue);
            return null;
        }
    }

    private Long parseRequiredLong(Object rawValue, String paramName) {
        Long val = parseOptionalLong(rawValue);
        if (val == null) {
            throw new BusinessException(
                    "POINT_MESURE_OBLIGATOIRE",
                    "L'ID du point de mesure ('" + paramName + "') est obligatoire et doit être un nombre valide (ex: 1, 2).",
                    HttpStatus.BAD_REQUEST);
        }
        return val;
    }

    private PointMesure validerEtChargerPoint(Object rawPointId) {
        Long idPointMesure = parseRequiredLong(rawPointId, "idPointMesure");
        return pointMesureRepository.findByIdAndActifTrueAndDeletedAtIsNull(idPointMesure)
                .orElseThrow(() -> new BusinessException(
                        "POINT_MESURE_INACTIF",
                        "Le point de mesure (ID " + idPointMesure + ") n'existe pas ou n'est pas actif.",
                        HttpStatus.BAD_REQUEST));
    }

    private Metrique validerEtParseMetrique(String metriqueStr) {
        if (metriqueStr == null || metriqueStr.isBlank()
                || metriqueStr.equalsIgnoreCase("none")
                || metriqueStr.equalsIgnoreCase("null")
                || metriqueStr.equalsIgnoreCase("n/a")) {
            throw new BusinessException("METRIQUE_OBLIGATOIRE", "La métrique est obligatoire (TEMPERATURE ou HUMIDITE).", HttpStatus.BAD_REQUEST);
        }
        try {
            return Metrique.valueOf(metriqueStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("METRIQUE_INVALIDE", "Métrique invalide : '" + metriqueStr + "'. Valeurs acceptées : TEMPERATURE, HUMIDITE.", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalDateTime parseDateISO(String dateStr, String paramName) {
        if (dateStr == null || dateStr.isBlank()
                || dateStr.equalsIgnoreCase("none")
                || dateStr.equalsIgnoreCase("null")
                || dateStr.equalsIgnoreCase("n/a")) {
            throw new BusinessException("DATE_OBLIGATOIRE", "Le paramètre '" + paramName + "' est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        try {
            return LocalDateTime.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("FORMAT_DATE_INVALID", "Format ISO-8601 invalide pour '" + paramName + "' (" + dateStr + "). Format attendu: YYYY-MM-DDTHH:mm:ss", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean estAlerteSurPoint(Alerte alerte, Long idPointMesure) {
        if (alerte.getIdMesure() == null) return false;
        Optional<Mesure> mes = mesureRepository.findById(alerte.getIdMesure());
        return mes.isPresent() && mes.get().getPointMesure() != null && Objects.equals(mes.get().getPointMesure().getId(), idPointMesure);
    }
}
