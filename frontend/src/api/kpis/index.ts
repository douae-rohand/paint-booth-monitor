/**
 * KPIs API
 * Source Java : com.projet.kpis.dto.KpiResponseDTO + com.projet.kpis.controller.KpiController
 *
 * GET /api/kpis
 *   Paramètres obligatoires : pointMesureId + metrique + dateDebut + dateFin
 */
import apiClient from '../../lib/axios';
import type { Metrique } from '../alerting/seuils';

// ── Types (miroir exact de KpiResponseDTO.java) ────────────────────────────────

/**
 * Réponse de GET /api/kpis.
 * tauxConformite, tempsMoyenEntreIncidentsHeures, tempsMoyenRetourNormalHeures
 * sont null si les données sont insuffisantes sur la période.
 */
export interface KpiResponseDTO {
  /** Nombre d'alertes actives (statut = ACTIVE) pour ce point/métrique sur la période. */
  alertesActives: number;
  /** Taux de conformité en % — null si pas de seuil configuré ou aucune mesure. */
  tauxConformite: number | null;
  /** Temps moyen entre incidents en heures — null si < 2 alertes SEUIL_ABSOLU sur la période. */
  tempsMoyenEntreIncidentsHeures: number | null;
  /** Temps moyen de retour à la normale en heures — null si aucune alerte résolue sur la période. */
  tempsMoyenRetourNormalHeures: number | null;
}

export interface KpiParams {
  pointMesureId?: number;
  metrique?: Metrique;
  /** Format ISO-8601 : "yyyy-MM-dd'T'HH:mm:ss" — requis */
  dateDebut?: string;
  /** Format ISO-8601 : "yyyy-MM-dd'T'HH:mm:ss" — requis */
  dateFin?: string;
}

// ── Fonctions API ──────────────────────────────────────────────────────────────

/**
 * GET /api/kpis — pointMesureId + metrique + dateDebut + dateFin obligatoires.
 */
export const getKpis = async (params?: KpiParams): Promise<KpiResponseDTO> => {
  const response = await apiClient.get<KpiResponseDTO>('/api/kpis', { params });
  return response.data;
};
