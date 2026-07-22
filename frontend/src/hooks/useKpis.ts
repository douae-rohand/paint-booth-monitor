/**
 * useKpis – performance indicators (conformance rate, MTBI, MTTR)
 */
import { useState, useEffect } from 'react';
import { getKpis } from '../api/kpis/index';
import type { KpiData } from '../api/kpis/index';

export const useKpis = () => {
  const [data, setData] = useState<KpiData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getKpis();
        setData(result);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, []);

  return { data, loading, error };
};
