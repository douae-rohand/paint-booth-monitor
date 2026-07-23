import client from '../lib/axios';
import type { KPI } from '../types';

export const getDashboardKPIs = async (): Promise<KPI> => {
  const response = await client.get<KPI>('/api/kpis/dashboard');
  return response.data;
};
