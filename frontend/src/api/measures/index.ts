/**
 * MEASURES API – real-time + history (temperature / humidity)
 * Consumes: GET /api/measures/latest, GET /api/measures/history
 */
import apiClient from '../../lib/axios';
import type { Measure } from '../../types';

export interface HistoryParams {
  page?: number;
  limit?: number;
  startDate?: string;
  endDate?: string;
  boxId?: string;
}

export interface HistoryResponse {
  measures: Measure[];
  total: number;
  page: number;
  limit: number;
}

export const getLatestMeasures = async (): Promise<Measure> => {
  const response = await apiClient.get<Measure>('/api/measures/latest');
  return response.data;
};

export const getHistoryMeasures = async (params?: HistoryParams): Promise<HistoryResponse> => {
  const response = await apiClient.get<HistoryResponse>('/api/measures/history', { params });
  return response.data;
};
