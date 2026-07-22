/**
 * ALERTING API – active/history alerts, seuils absolus & dynamiques
 * Consumes: GET /api/alerts, POST /api/alerts/:id/acknowledge, GET/POST/PUT /api/thresholds
 */
import apiClient from '../../lib/axios';
import type { Alert } from '../../types';

export interface AlertsParams {
  page?: number;
  limit?: number;
  acknowledged?: boolean;
  severity?: string;
  startDate?: string;
  endDate?: string;
}

export interface AlertsResponse {
  alerts: Alert[];
  total: number;
  page: number;
  limit: number;
}

export const getAlerts = async (params?: AlertsParams): Promise<AlertsResponse> => {
  const response = await apiClient.get<AlertsResponse>('/api/alerts', { params });
  return response.data;
};

export const acknowledgeAlert = async (alertId: string): Promise<Alert> => {
  const response = await apiClient.post<Alert>(`/api/alerts/${alertId}/acknowledge`);
  return response.data;
};

// ── Thresholds (admin only) ────────────────────────────────────────────────
export interface Threshold {
  id?: string;
  metric: 'temperature' | 'humidity';
  type: 'absolute' | 'dynamic';
  minValue?: number;
  maxValue?: number;
}

export const getThresholds = async (): Promise<Threshold[]> => {
  const response = await apiClient.get<Threshold[]>('/api/thresholds');
  return response.data;
};

export const updateThreshold = async (id: string, data: Partial<Threshold>): Promise<Threshold> => {
  const response = await apiClient.put<Threshold>(`/api/thresholds/${id}`, data);
  return response.data;
};
