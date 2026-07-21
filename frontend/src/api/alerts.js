import client from './client';

export const getAlerts = async (params) => {
  const response = await client.get('/api/alerts', { params });
  return response.data;
};

export const acknowledgeAlert = async (alertId) => {
  const response = await client.post(`/api/alerts/${alertId}/acknowledge`);
  return response.data;
};
