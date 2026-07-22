/**
 * useAlerting – alert list and threshold management
 */
import { useState, useEffect } from 'react';
import { getAlerts, type AlertsParams, type AlertsResponse } from '../api/alerting';

export const useAlerts = (params?: AlertsParams) => {
  const [data, setData] = useState<AlertsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getAlerts(params);
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
