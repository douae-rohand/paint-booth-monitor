package com.projet.kpis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les KPIs scopés (point de mesure + métrique + période).
 *
 * <p>{@code GET /api/kpis} exige les quatre paramètres (pointMesureId, metrique,
 * dateDebut, dateFin). Il n'existe pas de mode global.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponseDTO {

    /**
     * Nombre d'alertes actuellement actives (statut = ACTIVE) pour ce point/métrique.
     *
     * <p><strong>Instantané indépendant de la période sélectionnée</strong> — contrairement
     * aux 3 autres champs du DTO qui agrègent des données sur dateDebut/dateFin, ce champ
     * reflète l'état en temps réel : toutes les alertes de statut ACTIVE pour ce point et
     * cette métrique, quelle que soit leur date de création.
     *
     * <p>Ce champ est le seul véritablement "en direct" du DTO. Les 3 autres champs
     * (tauxConformite, tempsMoyenEntreIncidentsHeures, tempsMoyenRetourNormalHeures) sont
     * des agrégats recalculés sur la période — ils se rafraîchissent à chaque signal
     * WebSocket mais leur valeur dépend de la fenêtre temporelle choisie.
     */
    private Long alertesActives;

    /**
     * Taux de conformité en pourcentage sur la période.
     * Chaque mesure est comparée au SeuilAbsolu actif au moment de sa création
     * (via jointure LATERAL historisée), pas au seuil actif aujourd'hui.
     * Null si aucune mesure ou aucun seuil n'était actif sur la période.
     */
    private Double tauxConformite;

    /**
     * Temps moyen entre incidents (MTBI) en heures, sur la période sélectionnée.
     *
     * <p><strong>Périmètre délibérément limité aux alertes SEUIL_ABSOLU</strong> —
     * les alertes SEUIL_DYNAMIQUE sont exclues par choix de conception : le MTBI
     * mesure la fréquence des dépassements de seuils stables et configurés, pas
     * les anomalies statistiques du seuil dynamique dont la nature est différente.
     *
     * <p>Ce choix est assumé et documenté. Le module chatbot qui utilisera ce champ
     * doit en tenir compte : "temps moyen entre incidents" signifie ici "entre alertes
     * de type SEUIL_ABSOLU uniquement". Reformuler en conséquence dans les réponses
     * destinées à l'utilisateur.
     *
     * <p>Null si moins de 2 alertes SEUIL_ABSOLU sur la période.
     */
    private Double tempsMoyenEntreIncidentsHeures;

    /**
     * Temps moyen de retour à la normale en heures.
     * Filtré par date de résolution (updatedAt) — mesure les résolutions
     * survenues pendant la période, pas les créations.
     * Null si aucune alerte résolue pendant la période.
     */
    private Double tempsMoyenRetourNormalHeures;
}
