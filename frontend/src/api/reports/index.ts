/**
 * REPORTS API – daily PDF reports
 * Consumes: GET /api/reports, GET /api/reports/:id/download
 */
import apiClient from '../../lib/axios';

export interface Report {
  id: string;
  date: string;
  filename: string;
  downloadUrl: string;
}

export const getReports = async (): Promise<Report[]> => {
  const response = await apiClient.get<Report[]>('/api/reports');
  return response.data;
};

export const downloadReport = async (reportId: string): Promise<Blob> => {
  const response = await apiClient.get(`/api/reports/${reportId}/download`, {
    responseType: 'blob',
  });
  return response.data;
};
