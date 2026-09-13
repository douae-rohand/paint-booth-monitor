package com.projet.chatbot.tools;

import com.projet.config.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Outil déterministe de résolution des plages temporelles relatives en Java pur.
 * Utilisé par le chatbot pour convertir des expressions comme "hier", "cette semaine",
 * "les 7 derniers jours" en dates précises au format ISO-8601 (LocalDateTime).
 * Module: chatbot
 */
@Component
public class ResoudrePeriodeTool {

    public record PeriodeResult(
            String dateDebut,
            String dateFin,
            String description
    ) {}

    private static final Pattern PATTERN_DERNIERS_JOURS = Pattern.compile("(?:les\\s+)?(\\d+)\\s*(?:derniers?\\s+)?jours?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DERNIERES_HEURES = Pattern.compile("(?:les\\s+)?(\\d+)\\s*(?:dernieres?\\s+)?heures?", Pattern.CASE_INSENSITIVE);

    /**
     * Résout une expression textuelle en une plage de dates (dateDebut et dateFin).
     *
     * @param expression Expression relative (ex. "hier", "cette semaine", "les 7 derniers jours")
     * @return PeriodeResult avec dateDebut et dateFin au format ISO-8601 String
     */
    public PeriodeResult resoudre(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new BusinessException(
                    "PERIODE_INVALIDE",
                    "L'expression temporelle ne peut pas être vide.",
                    HttpStatus.BAD_REQUEST);
        }

        String exprNorm = expression.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // 1. Aujourd'hui / ce jour
        if (exprNorm.contains("aujourd'hui") || exprNorm.contains("aujourdhui") || exprNorm.contains("ce jour")) {
            LocalDateTime debut = LocalDate.now().atStartOfDay();
            return new PeriodeResult(debut.toString(), now.toString(), "Aujourd'hui de 00:00 à maintenant");
        }

        // 2. Hier
        if (exprNorm.contains("hier")) {
            LocalDate hier = LocalDate.now().minusDays(1);
            LocalDateTime debut = hier.atStartOfDay();
            LocalDateTime fin = hier.atTime(LocalTime.MAX);
            return new PeriodeResult(debut.toString(), fin.toString(), "Hier toute la journée");
        }

        // 3. La semaine dernière
        if (exprNorm.contains("semaine derniere") || exprNorm.contains("semaine dernière")) {
            LocalDate lundiDernier = LocalDate.now().minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate dimancheDernier = lundiDernier.plusDays(6);
            LocalDateTime debut = lundiDernier.atStartOfDay();
            LocalDateTime fin = dimancheDernier.atTime(LocalTime.MAX);
            return new PeriodeResult(debut.toString(), fin.toString(), "La semaine dernière (du lundi au dimanche)");
        }

        // 4. Cette semaine
        if (exprNorm.contains("cette semaine")) {
            LocalDate lundiCourant = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDateTime debut = lundiCourant.atStartOfDay();
            return new PeriodeResult(debut.toString(), now.toString(), "Cette semaine depuis lundi 00:00");
        }

        // 5. Ce mois-ci
        if (exprNorm.contains("ce mois")) {
            LocalDate premierDuMois = LocalDate.now().withDayOfMonth(1);
            LocalDateTime debut = premierDuMois.atStartOfDay();
            return new PeriodeResult(debut.toString(), now.toString(), "Ce mois-ci depuis le 1er du mois");
        }

        // 6. Les N derniers jours / N jours
        Matcher matcherJours = PATTERN_DERNIERS_JOURS.matcher(exprNorm);
        if (matcherJours.find()) {
            int nbJours = Integer.parseInt(matcherJours.group(1));
            LocalDateTime debut = now.minusDays(nbJours);
            return new PeriodeResult(debut.toString(), now.toString(), "Les " + nbJours + " derniers jours");
        }

        // 7. Les N dernières heures / N heures
        Matcher matcherHeures = PATTERN_DERNIERES_HEURES.matcher(exprNorm);
        if (matcherHeures.find()) {
            int nbHeures = Integer.parseInt(matcherHeures.group(1));
            LocalDateTime debut = now.minusHours(nbHeures);
            return new PeriodeResult(debut.toString(), now.toString(), "Les " + nbHeures + " dernières heures");
        }

        throw new BusinessException(
                "EXPRESSION_TEMPORELLE_INCONNUE",
                "Impossible de résoudre l'expression temporelle : \"" + expression + "\". Expressions supportées : 'aujourd'hui', 'hier', 'cette semaine', 'la semaine dernière', 'ce mois-ci', 'les N derniers jours'.",
                HttpStatus.BAD_REQUEST);
    }
}
