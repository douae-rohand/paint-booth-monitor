import client from './client';

export const getLatestMeasures = async () => {
  const response = await client.get('/api/measures/latest');
  return response.data;
};

export const getHistoryMeasures = async (params) => {
  const response = await client.get('/api/measures/history', { params });
  return response.data;
};
