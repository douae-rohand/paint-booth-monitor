/**
 * ADMIN API – user management, threshold config, notifications config
 * All endpoints require ROLE_ADMIN → protected at route level via beforeLoad.
 * Consumes: /api/admin/users, /api/admin/notifications
 */
import apiClient from '../../lib/axios';

export interface SuperviseurDto {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  actif: boolean;
  createdAt: string;
}

export interface CreateSuperviseurDto {
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

// ── Users management ───────────────────────────────────────────────────
export const getUsers = async (): Promise<SuperviseurDto[]> => {
  const response = await apiClient.get<SuperviseurDto[]>('/api/admin/users');
  return response.data;
};

export const createUser = async (data: CreateSuperviseurDto): Promise<SuperviseurDto> => {
  const response = await apiClient.post<SuperviseurDto>('/api/admin/users', data);
  return response.data;
};

export const deactivateUser = async (userId: string): Promise<void> => {
  await apiClient.put(`/api/admin/users/${userId}/deactivate`);
};
