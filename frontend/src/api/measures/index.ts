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

// ── Types Historique Cabine/Étuve (pagination) ───────────────────────────────────

/**
 * Miroir de MesureCabineDTO.java.
 * `timestampCycle` : format "yyyy-MM-dd'T'HH:mm:ss" (LocalDateTime Java).
 * `caisseId` : null pour l'instant (champ réservé).
 * `temperature` / `humidite` : BigDecimal Java → number | null.
 */
export interface MesureCabineDTO {
  /** Format ISO-8601 */
  timestampCycle: string;
  caisseId: string | null;
  temperature: number | null;
  humidite: number | null;
  depassementTemperature: boolean;
  depassementHumidite: boolean;
}

/**
 * Miroir de MesureEtuveDTO.java.
 * `idMesure` : UUID Java → string.
 * `dateMesure` : format "yyyy-MM-dd'T'HH:mm:ss" (LocalDateTime Java).
 * `zone` : nom du PointMesure (ex "Zone 1").
 * `temperature` : BigDecimal Java → number | null.
 */
export interface MesureEtuveDTO {
  idMesure: string;
  /** Format ISO-8601 */
  dateMesure: string;
  zone: string;
  temperature: number | null;
  depassement: boolean;
}

/**
 * Wrapper de pagination Spring Data (Page<T>).
 * Utilisé pour les réponses paginées des endpoints /cabine et /etuve.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface HistoriqueCabineParams {
  /** Format ISO-8601 (optionnel) */
  dateDebut?: string;
  /** Format ISO-8601 (optionnel) */
  dateFin?: string;
  seulementDepassements?: boolean;
  page?: number;
  size?: number;
}

export interface HistoriqueEtuveParams {
  zone?: string;
  /** Format ISO-8601 (optionnel) */
  dateDebut?: string;
  /** Format ISO-8601 (optionnel) */
  dateFin?: string;
  seulementDepassements?: boolean;
  page?: number;
  size?: number;
}

// ── Types Export ───────────────────────────────────────────────────────────────

export interface ExportParams {
  format: 'csv' | 'pdf' | 'xlsx';
  /** Format ISO-8601 (optionnel) */
  dateDebut?: string;
  /** Format ISO-8601 (optionnel) */
  dateFin?: string;
  seulementDepassements?: boolean;
}

export interface ExportCabineParams extends ExportParams {}

export interface ExportEtuveParams extends ExportParams {
  zone?: string;
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
 * GET /api/mesures/historique/cabine
 * Récupère l'historique des mesures de la cabine avec pivot température/humidité par cycle.
 * Les paramètres dateDebut/dateFin sont optionnels (sans valeur par défaut).
 */
export const getHistoriqueCabine = async (params: HistoriqueCabineParams): Promise<Page<MesureCabineDTO>> => {
  // Filtrer les paramètres undefined/null pour ne pas les envoyer dans l'URL
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([_, value]) => value !== undefined && value !== null)
  );
  const response = await apiClient.get<Page<MesureCabineDTO>>('/api/mesures/historique/cabine', { params: filteredParams });
  return response.data;
};

/**
 * GET /api/mesures/historique/etuve
 * Récupère l'historique des mesures de l'étuve par zone.
 * Les paramètres zone/dateDebut/dateFin sont optionnels (sans valeur par défaut).
 */
export const getHistoriqueEtuve = async (params: HistoriqueEtuveParams): Promise<Page<MesureEtuveDTO>> => {
  // Filtrer les paramètres undefined/null pour ne pas les envoyer dans l'URL
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([_, value]) => value !== undefined && value !== null)
  );
  const response = await apiClient.get<Page<MesureEtuveDTO>>('/api/mesures/historique/etuve', { params: filteredParams });
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

/**
 * GET /api/mesures/historique/cabine/export
 * Exporte l'historique des mesures de la cabine en CSV, PDF ou Excel.
 * Retourne un Blob pour le téléchargement.
 */
export const exportHistoriqueCabine = async (params: ExportCabineParams): Promise<Blob> => {
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([_, value]) => value !== undefined && value !== null)
  );
  const response = await apiClient.get('/api/mesures/historique/cabine/export', {
    params: filteredParams,
    responseType: 'blob'
  });
  return response.data;
};

/**
 * GET /api/mesures/historique/etuve/export
 * Exporte l'historique des mesures de l'étuve en CSV, PDF ou Excel.
 * Retourne un Blob pour le téléchargement.
 */
export const exportHistoriqueEtuve = async (params: ExportEtuveParams): Promise<Blob> => {
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([_, value]) => value !== undefined && value !== null)
  );
  const response = await apiClient.get('/api/mesures/historique/etuve/export', {
    params: filteredParams,
    responseType: 'blob'
  });
  return response.data;
};
