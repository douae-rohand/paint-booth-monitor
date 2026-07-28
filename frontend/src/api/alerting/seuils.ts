/**
 * SEUILS API – seuils absolus & dynamiques
 * Consumes: GET /api/seuils/absolus/active, /api/seuils/absolus/history, POST /api/seuils/absolus,
 *          PATCH /api/seuils/absolus/{id}/activer, PATCH /api/seuils/absolus/{id}/desactiver,
 *          GET /api/seuils/dynamiques, POST /api/seuils/dynamiques, PATCH /api/seuils/dynamiques/{id}
 */
import apiClient from '../../lib/axios';

// ── Types ──────────────────────────────────────────────────────────────────────

export interface PointMesure {
  id: number;
  nom: string;
  typeEmplacement: string;
  actif: boolean;
  dateCreation: string;
}

export type Metrique = 'TEMPERATURE' | 'HUMIDITE';

export interface SeuilAbsoluResponseDTO {
  id: string;
  idPointMesure: number;
  nomPointMesure: string;
  metrique: Metrique;
  valeurMin: number;
  valeurMax: number;
  actif: boolean;
  createdAt: string;
  dateActivation: string | null;
  dateDesactivation: string | null;
}

export interface SeuilAbsoluCreateDTO {
  idPointMesure: number;
  metrique: Metrique;
  valeurMin: number;
  valeurMax: number;
}

export interface SeuilDynamiqueResponseDTO {
  id: string;
  idAdmin: string;
  idPointMesure: number;
  metrique: Metrique;
  valeurMinCalculee: number | null;
  valeurMaxCalculee: number | null;
  margeConfiguree: number;
  dateCalcul: string | null;
  dateCreation: string;
  dateModification: string | null;
  dateSuppression: string | null;
}

export interface SeuilDynamiqueCreateDTO {
  idPointMesure: number;
  metrique: Metrique;
  margeConfiguree: number;
}

export interface SeuilDynamiqueUpdateDTO {
  margeConfiguree: number;
}

// ── PointMesure ─────────────────────────────────────────────────────────────────

export const getPointMesures = async (): Promise<PointMesure[]> => {
  const response = await apiClient.get<PointMesure[]>('/api/point-mesures');
  return response.data;
};

// ── Seuil Absolu ────────────────────────────────────────────────────────────────

export const getSeuilAbsoluActif = async (
  pointMesureId: number,
  metrique: Metrique,
): Promise<SeuilAbsoluResponseDTO | null> => {
  try {
    const response = await apiClient.get<SeuilAbsoluResponseDTO>('/api/seuils/absolus/active', {
      params: { pointMesureId, metrique },
    });
    return response.data;
  } catch (error: unknown) {
    if (typeof error === 'object' && error !== null && 'response' in error) {
      const err = error as any;
      if (err.response?.status === 404) {
        return null;
      }
    }
    throw error;
  }
};

export const getSeuilAbsoluHistory = async (
  pointMesureId: number,
  metrique: Metrique,
): Promise<SeuilAbsoluResponseDTO[]> => {
  const response = await apiClient.get<SeuilAbsoluResponseDTO[]>('/api/seuils/absolus/history', {
    params: { pointMesureId, metrique },
  });
  return response.data;
};

export const createSeuilAbsolu = async (
  data: SeuilAbsoluCreateDTO,
): Promise<SeuilAbsoluResponseDTO> => {
  const response = await apiClient.post<SeuilAbsoluResponseDTO>('/api/seuils/absolus', data);
  return response.data;
};

export const activerSeuilAbsolu = async (id: string): Promise<SeuilAbsoluResponseDTO> => {
  const response = await apiClient.patch<SeuilAbsoluResponseDTO>(`/api/seuils/absolus/${id}/activer`);
  return response.data;
};

export const desactiverSeuilAbsolu = async (id: string): Promise<SeuilAbsoluResponseDTO> => {
  const response = await apiClient.patch<SeuilAbsoluResponseDTO>(`/api/seuils/absolus/${id}/desactiver`);
  return response.data;
};

// ── Seuil Dynamique ─────────────────────────────────────────────────────────────

export const getSeuilDynamique = async (
  pointMesureId: number,
  metrique: Metrique,
): Promise<SeuilDynamiqueResponseDTO | null> => {
  try {
    const response = await apiClient.get<SeuilDynamiqueResponseDTO>('/api/seuils/dynamiques', {
      params: { pointMesureId, metrique },
    });
    return response.data;
  } catch (error: unknown) {
    if (typeof error === 'object' && error !== null && 'response' in error) {
      const err = error as any;
      if (err.response?.status === 404) {
        return null;
      }
    }
    throw error;
  }
};

export const createSeuilDynamique = async (
  data: SeuilDynamiqueCreateDTO,
): Promise<SeuilDynamiqueResponseDTO> => {
  const response = await apiClient.post<SeuilDynamiqueResponseDTO>('/api/seuils/dynamiques', data);
  return response.data;
};

export const updateSeuilDynamique = async (
  id: string,
  data: SeuilDynamiqueUpdateDTO,
): Promise<SeuilDynamiqueResponseDTO> => {
  const response = await apiClient.patch<SeuilDynamiqueResponseDTO>(`/api/seuils/dynamiques/${id}`, data);
  return response.data;
};
