import apiClient from '../../lib/axios';

export interface PlcConfiguration {
  id: string;
  ip: string;
  port: number;
  rack: number;
  slot: number;
  intervallePolling: number;
  actif: boolean;
  dateCreation: string;
  dateActivation: string | null;
  dateDesactivation: string | null;
}

export interface CreatePlcConfigurationRequest {
  ip: string;
  port: number;
  rack: number;
  slot: number;
  intervallePolling: number;
}

export const getActivePlcConfiguration = async (): Promise<PlcConfiguration | null> => {
  try {
    const response = await apiClient.get<PlcConfiguration>('/api/config/plc/active');
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

export const getPlcConfigurationHistory = async (): Promise<PlcConfiguration[]> => {
  const response = await apiClient.get<PlcConfiguration[]>('/api/config/plc/history');
  return response.data;
};

export const createPlcConfiguration = async (
  data: CreatePlcConfigurationRequest,
): Promise<PlcConfiguration> => {
  const response = await apiClient.post<PlcConfiguration>('/api/config/plc', data);
  return response.data;
};

export const activatePlcConfiguration = async (id: string): Promise<PlcConfiguration> => {
  const response = await apiClient.patch<PlcConfiguration>(`/api/config/plc/${id}/activer`);
  return response.data;
};

export const deactivatePlcConfiguration = async (id: string): Promise<PlcConfiguration> => {
  const response = await apiClient.patch<PlcConfiguration>(`/api/config/plc/${id}/desactiver`);
  return response.data;
};
