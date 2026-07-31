/**
 * KPIs API
 * Source Java : com.projet.kpis.dto.KpiResponseDTO + com.projet.kpis.controller.KpiController
 *
 * GET /api/kpis
 *   - Sans params : retourne alertesActives, nbPointsEnAnomalie, nbPointsTotal (champs scopés = null)
 *   - Avec pointMesureId + metrique + dateDebut + dateFin : retourne tous les champs
 */
import apiClient from '../../lib/axios';
import type { Metrique } from '../alerting/seuils';

// ── Types (miroir exact de KpiResponseDTO.java) ────────────────────────────────

/**
 * Réponse de GET /api/kpis.
 * Les champs tauxConformite, tempsMoyenEntreIncidentsHeures, tempsMoyenRetourNormalHeures
 * sont null si aucun scope point+métrique n'est fourni, ou si les données sont insuffisantes.
 */
export interface KpiResponseDTO {
  /** Nombre d'alertes actives (statut = ACTIVE). Instantané, pas lié à la période. */
  alertesActives: number;
  /** Nombre de points de mesure en anomalie (≥ 1 alerte active). */
  nbPointsEnAnomalie: number;
  /** Nombre total de points de mesure actifs non supprimés. */
  nbPointsTotal: number;
  /** Taux de conformité en % — null si pas de scope ou pas de seuil configuré. */
  tauxConformite: number | null;
  /** Temps moyen entre incidents en heures — null si < 2 alertes SEUIL_ABSOLU sur la période. */
  tempsMoyenEntreIncidentsHeures: number | null;
  /** Temps moyen de retour à la normale en heures — null si aucune alerte résolue. */
  tempsMoyenRetourNormalHeures: number | null;
}

export interface KpiParams {
  pointMesureId?: number;
  metrique?: Metrique;
  /** Format ISO-8601 : "yyyy-MM-dd'T'HH:mm:ss" — requis si pointMesureId+metrique fournis */
  dateDebut?: string;
  /** Format ISO-8601 : "yyyy-MM-dd'T'HH:mm:ss" — requis si pointMesureId+metrique fournis */
  dateFin?: string;
}

// ── Fonctions API ──────────────────────────────────────────────────────────────

/**
 * GET /api/kpis
 * Si params vides → scope global (champs scopés null).
 * Si pointMesureId + metrique + dateDebut + dateFin → scope complet.
 */
export const getKpis = async (params?: KpiParams): Promise<KpiResponseDTO> => {
  const response = await apiClient.get<KpiResponseDTO>('/api/kpis', { params });
  return response.data;
};
