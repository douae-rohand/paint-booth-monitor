/**
 * MEASURES API
 * Sources Java :
 *  - com.projet.measures.controller.StatutTempsReelController  → GET /api/mesures/temps-reel
 *  - com.projet.measures.controller.MesureHistoriqueController → GET /api/mesures/historique
 *  - com.projet.measures.controller.PointMesureController       → GET /api/point-mesures
 *  - com.projet.measures.dto.PointMesureStatutDTO
 *  - com.projet.measures.dto.MesureHistoriqueResponseDTO
 *  - com.projet.measures.dto.MesureHistoriqueDTO
 *  - com.projet.measures.dto.PointMesureResponse
 */
import apiClient from '../../lib/axios';
import type { Metrique } from '../alerting/seuils';

// ── Enums (miroir exact des enums Java) ────────────────────────────────────────

/** Miroir de PointMesureStatutDTO.StatutMesure */
export type StatutMesure = 'CRITIQUE' | 'ATTENTION' | 'NOMINAL' | 'INCONNU';

// ── Types Statut Temps Réel ────────────────────────────────────────────────────

/**
 * Miroir de PointMesureStatutDTO.MesureStatutDTO (classe interne).
 * Champ `derniereValeur` : BigDecimal Java → number | null côté TS.
 * Champ `dateDerniereMesure` : format "yyyy-MM-dd'T'HH:mm:ss" (annotation @JsonFormat Java).
 */
export interface MesureStatutDTO {
  metrique: Metrique;
  derniereValeur: number | null;
  /** Format "yyyy-MM-dd'T'HH:mm:ss" */
  dateDerniereMesure: string | null;
  statut: StatutMesure;
}

/**
 * Miroir de PointMesureStatutDTO.java.
 * La liste `mesures` contient une entrée par métrique applicable au point.
 * CABINE → [TEMPERATURE, HUMIDITE] ; ETUVE → [TEMPERATURE] uniquement.
 */
export interface PointMesureStatutDTO {
  idPointMesure: number;
  nomPointMesure: string;
  /** "CABINE" | "ETUVE" */
  typeEmplacement: string;
  mesures: MesureStatutDTO[];
}

// ── Types Historique ────────────────────────────────────────────────────────────

/** Miroir de Granularite.java */
export type Granularite = 'TRENTE_MIN' | 'HORAIRE' | 'JOURNALIERE' | 'MENSUELLE';

/**
 * Miroir de MesureHistoriqueDTO.java.
 * `horodatage` : format "yyyy-MM-dd'T'HH:mm:ss" (annotation @JsonFormat Java).
 * `valeur` : BigDecimal Java → number côté TS (valeur moyenne agrégée).
 */
export interface MesureHistoriqueDTO {
  /** Format "yyyy-MM-dd'T'HH:mm:ss" - horodatage tronqué selon granularité */
  horodatage: string;
  valeur: number;
}

/**
 * Miroir de MesureHistoriqueResponseDTO.java.
 * `points` : nom exact du champ Java (pas "data").
 * `seuilAbsolu` : null si aucun seuil configuré pour ce point+métrique.
 * `granulariteAppliquee` : granularité utilisée pour l'agrégation.
 */
export interface MesureHistoriqueResponseDTO {
  /** Liste des points triés par horodatage croissant. Nom Java : "points". */
  points: MesureHistoriqueDTO[];
  /** Seuil absolu actif. Null si aucun seuil configuré. */
  seuilAbsolu: {
    valeurMin: number;
    valeurMax: number;
  } | null;
  /** Granularité appliquée pour l'agrégation des données. */
  granulariteAppliquee: Granularite;
}

export interface HistoriqueParams {
  pointMesureId: number;
  metrique: Metrique;
  /** Période prédéfinie (24h, 7j, 30j, 6mois, 1an, personnalise) */
  periode: string;
  /** Format ISO-8601 */
  dateDebut: string;
  /** Format ISO-8601 */
  dateFin: string;
  /** Granularité demandée (optionnel, uniquement utilisé pour periode=7j) */
  granularite?: Granularite;
}

// ── Types PointMesure (liste) ─────────────────────────────────────────────────

/**
 * Miroir de PointMesureResponse.java.
 * `metriquesApplicables` : calculé côté Java selon typeEmplacement.
 *   CABINE → ["TEMPERATURE", "HUMIDITE"]
 *   ETUVE  → ["TEMPERATURE"]
 */
export interface PointMesureResponse {
  id: number;
  nom: string;
  /** "CABINE" | "ETUVE" */
  typeEmplacement: string;
  actif: boolean;
  dateCreation: string;
  updatedAt: string | null;
  deletedAt: string | null;
  metriquesApplicables: string[];
}

// ── Fonctions API ──────────────────────────────────────────────────────────────

/**
 * GET /api/mesures/temps-reel
 * Retourne le statut temps réel de tous les points de mesure actifs.
 */
export const getStatutTempsReel = async (): Promise<PointMesureStatutDTO[]> => {
  const response = await apiClient.get<PointMesureStatutDTO[]>('/api/mesures/temps-reel');
  return response.data;
};

/**
 * GET /api/mesures/historique
 * Tous les paramètres sont requis (pointMesureId, metrique, dateDebut, dateFin).
 */
export const getMesuresHistorique = async (params: HistoriqueParams): Promise<MesureHistoriqueResponseDTO> => {
  const response = await apiClient.get<MesureHistoriqueResponseDTO>('/api/mesures/historique', { params });
  return response.data;
};

/**
 * GET /api/point-mesures
 * Retourne tous les points de mesure actifs avec leurs métriques applicables.
 * Note : endpoint listé sous /api/point-mesures (pas /api/point-mesure/{id}).
 */
export const getPointMesures = async (): Promise<PointMesureResponse[]> => {
  const response = await apiClient.get<PointMesureResponse[]>('/api/point-mesures');
  return response.data;
};
