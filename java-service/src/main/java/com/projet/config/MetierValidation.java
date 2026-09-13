package com.projet.config;

import com.projet.alerting.model.enums.Metrique;
import com.projet.measures.model.PointMesure;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Utilitaires de validation métier transverses.
 *
 * Centralisé dans com.projet.config car ces validations sont partagées par
 * plusieurs modules (measures, kpis, reports, chatbot) et n'appartiennent
 * à la responsabilité d'aucun module en particulier.
 *
 * Toutes les méthodes sont statiques — pas d'état, pas d'injection nécessaire.
 * Les erreurs de validation lèvent {@link BusinessException} avec un code
 * SNAKE_CASE distinct par cas, permettant au frontend et au module chatbot
 * de distinguer précisément chaque type d'erreur.
 */
public final class MetierValidation {

    /**
     * Durée maximale autorisée pour un appel non déterministe (ex. LLM).
     *
     * <p>Cette constante est pensée pour les outils du module chatbot, où un
     * modèle de langage peut demander n'importe quelle période sans contrainte
     * sémantique. Elle n'est <em>pas</em> censée s'appliquer par défaut aux
     * flux humains existants (dashboard, rapports), dont les périodes sont
     * bornées par l'interface utilisateur. Si un flux existant doit adopter
     * cette limite, cela nécessite une décision explicite.
     */
    public static final int MAX_JOURS_PERIODE = 365;

    private MetierValidation() {
        // Classe utilitaire — instanciation interdite
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation métrique ↔ type d'emplacement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie qu'une métrique est applicable au type d'emplacement d'un point de mesure.
     *
     * <p>Règles actuelles :
     * <ul>
     *   <li>CABINE → TEMPERATURE et HUMIDITE acceptées</li>
     *   <li>ETUVE  → TEMPERATURE uniquement (HUMIDITE → {@link BusinessException})</li>
     * </ul>
     *
     * @param pointMesure Point de mesure dont le type d'emplacement est vérifié
     * @param metrique    Métrique demandée
     * @throws BusinessException code {@code "METRIQUE_NON_APPLICABLE"} (400) si invalide
     */
    public static void validerMetriqueApplicable(PointMesure pointMesure, Metrique metrique) {
        String type = pointMesure.getTypeEmplacement();
        if ("ETUVE".equalsIgnoreCase(type) && metrique == Metrique.HUMIDITE) {
            throw new BusinessException(
                    "METRIQUE_NON_APPLICABLE",
                    String.format(
                            "La métrique %s n'est pas applicable au point de mesure \"%s\" (type : %s).",
                            metrique, pointMesure.getNom(), type),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Retourne la liste des métriques applicables pour un type d'emplacement donné.
     *
     * <p>Usage inverse de {@link #validerMetriqueApplicable} : permet de
     * <em>découvrir</em> les métriques valides au lieu de valider une métrique connue.
     * Utilisé par les services qui itèrent sur toutes les métriques d'un point
     * (StatutTempsReelService, RapportGenerationService).
     *
     * @param typeEmplacement Type d'emplacement (insensible à la casse)
     * @return Liste non modifiable des métriques applicables ; liste vide si type inconnu
     */
    public static List<Metrique> metriquesApplicables(String typeEmplacement) {
        if ("CABINE".equalsIgnoreCase(typeEmplacement)) {
            return List.of(Metrique.TEMPERATURE, Metrique.HUMIDITE);
        } else if ("ETUVE".equalsIgnoreCase(typeEmplacement)) {
            return List.of(Metrique.TEMPERATURE);
        }
        return List.of();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation de plage de dates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie que {@code dateDebut} et {@code dateFin} sont toutes deux non nulles
     * et que {@code dateDebut} est strictement antérieure à {@code dateFin}.
     *
     * @param dateDebut Date de début de la période
     * @param dateFin   Date de fin de la période
     * @throws BusinessException code {@code "DATES_OBLIGATOIRES"} (400) si l'une est nulle
     * @throws BusinessException code {@code "DATES_INVALIDES"} (400) si dateDebut >= dateFin
     */
    public static void validerPlageDates(LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new BusinessException(
                    "DATES_OBLIGATOIRES",
                    "Les dates de début et de fin sont obligatoires.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!dateDebut.isBefore(dateFin)) {
            throw new BusinessException(
                    "DATES_INVALIDES",
                    "La date de début doit être strictement antérieure à la date de fin.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Variante conditionnelle de {@link #validerPlageDates} pour les cas où
     * {@code dateDebut} et {@code dateFin} sont <em>optionnelles</em>.
     *
     * <p>La validation ne s'applique que si les <em>deux</em> valeurs sont non nulles :
     * <ul>
     *   <li>Une seule fournie → aucune exception (borne partielle acceptée par le filtre SQL)</li>
     *   <li>Les deux fournies → {@code dateDebut} doit être strictement antérieure à {@code dateFin}</li>
     * </ul>
     *
     * @param dateDebut Date de début (peut être nulle)
     * @param dateFin   Date de fin (peut être nulle)
     * @throws BusinessException code {@code "DATES_INVALIDES"} (400) si les deux sont présentes
     *                           et {@code dateDebut} &gt;= {@code dateFin}
     */
    public static void validerPlageDatesOptionnelles(LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (dateDebut != null && dateFin != null && !dateDebut.isBefore(dateFin)) {
            throw new BusinessException(
                    "DATES_INVALIDES",
                    "La date de début doit être strictement antérieure à la date de fin.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Vérifie que la période définie par {@code dateDebut}/{@code dateFin} ne dépasse pas
     * {@link #MAX_JOURS_PERIODE} jours.
     *
     * <p><strong>Usage prévu :</strong> outils du module chatbot uniquement.
     * Ne pas appliquer aux flux dashboard ou rapports sans décision explicite —
     * ces flux acceptent des périodes multi-années par conception.
     *
     * <p>Prérequis : {@link #validerPlageDates} doit avoir été appelé avant cette
     * méthode pour garantir que les deux dates sont non nulles et ordonnées.
     *
     * @param dateDebut Date de début de la période (non nulle)
     * @param dateFin   Date de fin de la période (non nulle, après dateDebut)
     * @throws BusinessException code {@code "PERIODE_EXCESSIVE"} (400) si l'écart dépasse la limite
     */
    public static void validerDureeMaximale(LocalDateTime dateDebut, LocalDateTime dateFin) {
        long jours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        if (jours > MAX_JOURS_PERIODE) {
            throw new BusinessException(
                    "PERIODE_EXCESSIVE",
                    String.format(
                            "La période demandée (%d jours) dépasse la limite autorisée de %d jours.",
                            jours, MAX_JOURS_PERIODE),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
