import client from '../lib/axios';
import type { Measure } from '../types';

interface HistoryParams {
  page?: number;
  limit?: number;
  startDate?: string;
  endDate?: string;
  boxId?: string;
}

interface HistoryResponse {
  measures: Measure[];
  total: number;
  page: number;
  limit: number;
}

export const getLatestMeasures = async (): Promise<Measure> => {
  const response = await client.get<Measure>('/api/measures/latest');
  return response.data;
};

export const getHistoryMeasures = async (params?: HistoryParams): Promise<HistoryResponse> => {
  const response = await client.get<HistoryResponse>('/api/measures/history', { params });
  return response.data;
};
