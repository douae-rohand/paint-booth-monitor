/**
 * useAlerting – alert list and threshold management
 */
import { useState, useEffect } from 'react';
import { getHistoriqueAlertes, type AlertesParams, type AlertesPage } from '../api/alerting';

export const useAlerts = (params?: AlertesParams) => {
  const [data, setData] = useState<AlertesPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getHistoriqueAlertes(params);
        setData(result);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [JSON.stringify(params)]);

  return { data, loading, error };
};

