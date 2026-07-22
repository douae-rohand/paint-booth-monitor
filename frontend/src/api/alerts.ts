import client from './client';
import type { Alert } from '../types';

interface AlertsParams {
  page?: number;
  limit?: number;
  acknowledged?: boolean;
  severity?: string;
  startDate?: string;
  endDate?: string;
}

interface AlertsResponse {
  alerts: Alert[];
  total: number;
  page: number;
  limit: number;
}

export const getAlerts = async (params?: AlertsParams): Promise<AlertsResponse> => {
  const response = await client.get<AlertsResponse>('/api/alerts', { params });
  return response.data;
};

export const acknowledgeAlert = async (alertId: string): Promise<Alert> => {
  const response = await client.post<Alert>(`/api/alerts/${alertId}/acknowledge`);
  return response.data;
};
