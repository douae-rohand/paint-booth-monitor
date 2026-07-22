/**
 * useMeasures – real-time and historical temperature/humidity data
 */
import { useState, useEffect } from 'react';
import { getLatestMeasures, getHistoryMeasures } from '../api/measures/index';
import type { HistoryParams, HistoryResponse } from '../api/measures/index';
import type { Measure } from '../types';

export const useLatestMeasures = () => {
  const [data, setData] = useState<Measure | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getLatestMeasures();
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

export const useHistoryMeasures = (params?: HistoryParams) => {
  const [data, setData] = useState<HistoryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getHistoryMeasures(params);
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
