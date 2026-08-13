/**
 * ALERTES STATS API (dashboard)
 * Source Java : com.projet.alerting.controller.AlerteStatsController
 *               com.projet.alerting.dto.{TopAlerteDTO, HeatmapJourDTO, DetailJourAlertesDTO}
 *
 * GET /api/alertes/top
 * GET /api/alertes/heatmap
 * GET /api/alertes/heatmap/jour
 */
import apiClient from '../../lib/axios';
import type { Metrique } from '../alerting/seuils';

// ── Types (miroir exact des DTOs Java) ─────────────────────────────────────────

/**
 * Miroir de TopAlerteDTO.java.
 * `nombreDepassements` : Long Java → number TS.
 * `metrique` : enum Metrique Java → 'TEMPERATURE' | 'HUMIDITE'.
 */
export interface TopAlerteDTO {
  idPointMesure: number;
  nomPointMesure: string;
  metrique: Metrique;
  nombreDepassements: number;
}

/**
 * Miroir de HeatmapJourDTO.java.
 * `jour`  : numéro du jour du mois (1-31), conservé pour compatibilité.
 * `date`  : date complète au format "yyyy-MM-dd" — champ canonique pour le frontend.
 * `nombreAlertesCritiques` : Long Java → number TS.
 *   Ne compte que les alertes SEUIL_ABSOLU — cohérence avec la popup de détail.
 *   (Renommé depuis nombreDepassements pour refléter la sémantique exacte.)
 */
export interface HeatmapJourDTO {
  jour: number;
  /** Format "yyyy-MM-dd" */
  date: string;
  nombreAlertesCritiques: number;
}

/**
 * Miroir de DetailJourAlertesDTO.SeuilConfigureDTO (classe interne Java).
 * BigDecimal Java → number TS.
 */
export interface SeuilConfigureDTO {
  valeurMin: number;
  valeurMax: number;
}

/**
 * Miroir de DetailJourAlertesDTO.DetailAlerteDTO (classe interne Java).
 * `idPointMesure` : présent dans le DTO Java (vérifié).
 * `valeurMaxAtteinte` : BigDecimal Java → number TS.
 * `seuilConfigure` : null si aucun seuil actif configuré pour ce point+métrique.
 */
export interface DetailAlerteDTO {
  idPointMesure: number;
  nomPointMesure: string;
  metrique: Metrique;
  nombreDepassements: number;
  valeurMaxAtteinte: number;
  seuilConfigure: SeuilConfigureDTO | null;
}

/**
 * Miroir de DetailJourAlertesDTO.java.
 * `date` : format "yyyy-MM-dd" (annotation @JsonFormat Java).
 * `nombreTotalDepassements` : Long Java → number TS.
 */
export interface DetailJourAlertesDTO {
  /** Format "yyyy-MM-dd" */
  date: string;
  nombreTotalDepassements: number;
  details: DetailAlerteDTO[];
}

// ── Paramètres des requêtes ────────────────────────────────────────────────────

/**
 * Périodes prédéfinies acceptées par AlerteStatsController.calculerDateDebut().
 * "personnalise" → dateDebut + dateFin requis.
 */
export type PeriodeAlerte = '24h' | '7j' | '30j' | '3mois' | '6mois' | '1an' | 'personnalise';

export interface TopAlertesParams {
  /** Période prédéfinie — si absent, dateDebut+dateFin utilisées directement */
  periode?: PeriodeAlerte;
  /** Format ISO-8601 — requis si periode="personnalise" */
  dateDebut?: string;
  /** Format ISO-8601 — requis si periode="personnalise" */
  dateFin?: string;
  /** Filtre optionnel sur le point de mesure */
  pointMesureId?: number;
  /** Nombre max de résultats (défaut backend = 10) */
  limit?: number;
}

export interface HeatmapParams {
  /** Année (ex: 2026) */
  annee: number;
  /** Mois 1-12 */
  mois: number;
  pointMesureId?: number;
  metrique?: Metrique;
}

export interface HeatmapJourParams {
  /** Format "yyyy-MM-dd" */
  date: string;
  pointMesureId?: number;
  metrique?: Metrique;
}

// ── Fonctions API ──────────────────────────────────────────────────────────────

/**
 * GET /api/alertes/top
 */
export const getTopAlertes = async (params?: TopAlertesParams): Promise<TopAlerteDTO[]> => {
  const response = await apiClient.get<TopAlerteDTO[]>('/api/alertes/top', { params });
  return response.data;
};

/**
 * GET /api/alertes/heatmap
 */
export const getHeatmapMois = async (params: HeatmapParams): Promise<HeatmapJourDTO[]> => {
  const response = await apiClient.get<HeatmapJourDTO[]>('/api/alertes/heatmap', { params });
  return response.data;
};

/**
 * GET /api/alertes/heatmap/jour
 */
export const getHeatmapJour = async (params: HeatmapJourParams): Promise<DetailJourAlertesDTO> => {
  const response = await apiClient.get<DetailJourAlertesDTO>('/api/alertes/heatmap/jour', { params });
  return response.data;
};
