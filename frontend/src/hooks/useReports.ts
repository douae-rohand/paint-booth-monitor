/**
 * useReports – list and download daily PDF reports
 */
import { useState, useEffect } from 'react';
import { getReports, downloadReport, type Report } from '../api/reports';

export const useReports = () => {
  const [data, setData] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getReports();
        setData(result);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, []);

  const download = async (reportId: string, filename: string) => {
    const blob = await downloadReport(reportId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  return { data, loading, error, download };
};
