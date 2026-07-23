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

// Helper to map backend DTO (ConfigurationPLCResponse) to frontend interface
function mapPlcConfig(data: any): PlcConfiguration {
  if (!data) return data;
  return {
    id: data.idConfiguration ?? data.id,
    ip: data.plcIp ?? data.ip,
    port: data.plcPort ?? data.port,
    rack: data.plcRack ?? data.rack,
    slot: data.plcSlot ?? data.slot,
    intervallePolling: data.plcPollingIntervalMs ?? data.intervallePolling,
    actif: data.actif,
    dateCreation: data.dateCreation,
    dateActivation: data.dateActivation,
    dateDesactivation: data.dateDesactivation,
  };
}

export const getActivePlcConfiguration = async (): Promise<PlcConfiguration | null> => {
  try {
    const response = await apiClient.get<any>('/api/config/plc/active');
    return response.data ? mapPlcConfig(response.data) : null;
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
  const response = await apiClient.get<any[]>('/api/config/plc/history');
  return Array.isArray(response.data) ? response.data.map(mapPlcConfig) : [];
};

export const createPlcConfiguration = async (
  data: CreatePlcConfigurationRequest,
): Promise<PlcConfiguration> => {
  const payload = {
    ip: data.ip,
    port: data.port,
    rack: data.rack,
    slot: data.slot,
    intervallePolling: data.intervallePolling,
  };
  const response = await apiClient.post<any>('/api/config/plc', payload);
  return mapPlcConfig(response.data);
};

export const activatePlcConfiguration = async (id: string): Promise<PlcConfiguration> => {
  const response = await apiClient.patch<any>(`/api/config/plc/${id}/activer`);
  return mapPlcConfig(response.data);
};

export const deactivatePlcConfiguration = async (id: string): Promise<PlcConfiguration> => {
  const response = await apiClient.patch<any>(`/api/config/plc/${id}/desactiver`);
  return mapPlcConfig(response.data);
};
