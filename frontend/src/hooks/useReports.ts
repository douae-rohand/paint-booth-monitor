/**
 * useReports – list and download daily PDF reports
 */
import { useState, useEffect } from 'react';
import { listerRapports, telechargerRapport, type RapportPDFResponse } from '../api/reports';

export const useReports = () => {
  const [data, setData] = useState<RapportPDFResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await listerRapports();
        setData(result.content);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, []);

  const download = async (reportId: string, filename: string) => {
    await telechargerRapport(reportId, filename);
  };

  return { data, loading, error, download };
};

