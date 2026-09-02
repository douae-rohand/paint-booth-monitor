/**
 * ADMIN API – Superviseur management
 * All endpoints require ROLE_ADMIN → protected at route level via beforeLoad.
 * Consumes: /api/admin/superviseurs
 */
import apiClient from '../../lib/axios';

export interface SuperviseurResponseDTO {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  actif: boolean;
  compteActive: boolean;
  createdAt: string;
  dateExpirationActivation?: string | null;
}

export interface SuperviseurListItemDTO {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  actif: boolean;
  compteActive: boolean;
}

export interface SuperviseurCreateDTO {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
}

export interface SuperviseurUpdateDTO {
  nom?: string;
  prenom?: string;
  email?: string;
  telephone?: string;
}

export interface ActivationCompteDTO {
  token: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}

export interface SuperviseurPageResponse {
  content: SuperviseurListItemDTO[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ── Superviseur management ───────────────────────────────────────────────────
export const listSuperviseurs = async (params?: {
  actif?: boolean;
  compteActive?: boolean;
  page?: number;
  size?: number;
}): Promise<SuperviseurPageResponse> => {
  const response = await apiClient.get<SuperviseurPageResponse>('/api/admin/superviseurs', { params });
  return response.data;
};

export const getSuperviseur = async (id: string): Promise<SuperviseurResponseDTO> => {
  const response = await apiClient.get<SuperviseurResponseDTO>(`/api/admin/superviseurs/${id}`);
  return response.data;
};

export const createSuperviseur = async (data: SuperviseurCreateDTO): Promise<SuperviseurResponseDTO> => {
  const response = await apiClient.post<SuperviseurResponseDTO>('/api/admin/superviseurs', data);
  return response.data;
};

export const updateSuperviseur = async (id: string, data: SuperviseurUpdateDTO): Promise<SuperviseurResponseDTO> => {
  const response = await apiClient.patch<SuperviseurResponseDTO>(`/api/admin/superviseurs/${id}`, data);
  return response.data;
};

export const activerSuperviseur = async (id: string): Promise<void> => {
  await apiClient.patch(`/api/admin/superviseurs/${id}/activer`);
};

export const desactiverSuperviseur = async (id: string): Promise<void> => {
  await apiClient.patch(`/api/admin/superviseurs/${id}/desactiver`);
};

export const renvoyerActivationSuperviseur = async (id: string): Promise<SuperviseurResponseDTO> => {
  const response = await apiClient.post<SuperviseurResponseDTO>(`/api/admin/superviseurs/${id}/renvoyer-activation`);
  return response.data;
};

// ── Account activation (public) ───────────────────────────────────────────────
export const activerCompte = async (data: ActivationCompteDTO): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/api/auth/activation', data);
  return response.data;
};
