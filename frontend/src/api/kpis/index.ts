/**
 * KPIs API
 * Consumes: GET /api/kpis
 */
import apiClient from '../../lib/axios';

export interface KpiData {
  conformanceRate: { temperature: number; humidity: number };
  meanTimeBetweenIncidents: { temperature: number; humidity: number };
  meanTimeToRecover: { temperature: number; humidity: number };
}

export const getKpis = async (): Promise<KpiData> => {
  const response = await apiClient.get<KpiData>('/api/kpis');
  return response.data;
};
