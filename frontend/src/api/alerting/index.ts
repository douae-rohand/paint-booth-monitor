/**
 * ALERTING API – active/history alerts, seuils absolus & dynamiques
 * Consumes: GET /api/alertes, GET /api/alertes/actives, GET/POST/PUT /api/seuils
 */
import apiClient from '../../lib/axios';

// ── Types Alertes (backend Java) ───────────────────────────────────────────────

export interface AlerteDTO {
  idAlerte: string;
  dateCreation: string;
  dateResolution: string | null;
  pointMesureNom: string;
  metrique: string;
  typeAlerte: string;
  severite: string;
  statut: string;
  dureeMinutes: number;
}

export interface AlertesParams {
  statut?: string;
  typeAlerte?: string;
  severite?: string;
  idPointMesure?: number;
  dateDebut?: string;
  dateFin?: string;
  page?: number;
  size?: number;
}

export interface AlertesPage {
  content: AlerteDTO[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ── Alertes Consultation ───────────────────────────────────────────────────────

/**
 * GET /api/alertes/actives
 * Récupère toutes les alertes actives (sans pagination).
 */
export const getAlertesActives = async (): Promise<AlerteDTO[]> => {
  const response = await apiClient.get<AlerteDTO[]>('/api/alertes/actives');
  return response.data;
};

/**
 * GET /api/alertes
 * Récupère l'historique des alertes avec filtres optionnels et pagination.
 */
export const getHistoriqueAlertes = async (params?: AlertesParams): Promise<AlertesPage> => {
  const filteredParams = Object.fromEntries(
    Object.entries(params || {}).filter(([_, value]) => value !== undefined && value !== null)
  );
  const response = await apiClient.get<AlertesPage>('/api/alertes', { params: filteredParams });
  return response.data;
};

// ── Re-export from seuils.ts ─────────────────────────────────────────────────
export * from './seuils';
