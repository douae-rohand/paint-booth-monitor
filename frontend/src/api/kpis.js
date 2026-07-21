import client from './client';

export const getDashboardKPIs = async () => {
  const response = await client.get('/api/kpis/dashboard');
  return response.data;
};
