/**
 * useSeuils – threshold management (seuils absolus & dynamiques)
 */
import { useState, useEffect } from 'react';
import {
  getPointMesures,
  getSeuilAbsoluActif,
  getSeuilAbsoluHistory,
  createSeuilAbsolu,
  activerSeuilAbsolu,
  desactiverSeuilAbsolu,
  getSeuilDynamique,
  createSeuilDynamique,
  updateSeuilDynamique,
  type PointMesure,
  type Metrique,
  type SeuilAbsoluResponseDTO,
  type SeuilAbsoluCreateDTO,
  type SeuilDynamiqueResponseDTO,
  type SeuilDynamiqueCreateDTO,
  type SeuilDynamiqueUpdateDTO,
} from '../api/alerting/seuils';

// ── PointMesure ───────────────────────────────────────────────────────────────

export const usePointMesures = () => {
  const [data, setData] = useState<PointMesure[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getPointMesures();
        setData(result.filter((p) => p.actif));
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

// ── Seuil Absolu ────────────────────────────────────────────────────────────────

export const useSeuilAbsoluActif = (pointMesureId: number | null, metrique: Metrique | null) => {
  const [data, setData] = useState<SeuilAbsoluResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetch = async () => {
    if (!pointMesureId || !metrique) return;
    setLoading(true);
    try {
      const result = await getSeuilAbsoluActif(pointMesureId, metrique);
      setData(result);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetch();
  }, [pointMesureId, metrique]);

  return { data, loading, error, refetch: fetch };
};

export const useSeuilAbsoluHistory = (pointMesureId: number | null, metrique: Metrique | null) => {
  const [data, setData] = useState<SeuilAbsoluResponseDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetch = async () => {
    if (!pointMesureId || !metrique) return;
    setLoading(true);
    try {
      const result = await getSeuilAbsoluHistory(pointMesureId, metrique);
      setData(result);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetch();
  }, [pointMesureId, metrique]);

  return { data, loading, error, refetch: fetch };
};

export const useCreateSeuilAbsolu = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const create = async (data: SeuilAbsoluCreateDTO) => {
    setLoading(true);
    setError(null);
    try {
      const result = await createSeuilAbsolu(data);
      return result;
    } catch (e) {
      setError(e as Error);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { create, loading, error };
};

export const useToggleSeuilAbsolu = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const activer = async (id: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await activerSeuilAbsolu(id);
      return result;
    } catch (e) {
      setError(e as Error);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  const desactiver = async (id: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await desactiverSeuilAbsolu(id);
      return result;
    } catch (e) {
      setError(e as Error);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { activer, desactiver, loading, error };
};

// ── Seuil Dynamique ─────────────────────────────────────────────────────────────

export const useSeuilDynamique = (pointMesureId: number | null, metrique: Metrique | null) => {
  const [data, setData] = useState<SeuilDynamiqueResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetch = async () => {
    if (!pointMesureId || !metrique) return;
    setLoading(true);
    try {
      const result = await getSeuilDynamique(pointMesureId, metrique);
      setData(result);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetch();
  }, [pointMesureId, metrique]);

  return { data, loading, error, refetch: fetch };
};

export const useCreateSeuilDynamique = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const create = async (data: SeuilDynamiqueCreateDTO) => {
    setLoading(true);
    setError(null);
    try {
      const result = await createSeuilDynamique(data);
      return result;
    } catch (e) {
      setError(e as Error);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { create, loading, error };
};

export const useUpdateSeuilDynamique = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const update = async (id: string, data: SeuilDynamiqueUpdateDTO) => {
    setLoading(true);
    setError(null);
    try {
      const result = await updateSeuilDynamique(id, data);
      return result;
    } catch (e) {
      setError(e as Error);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { update, loading, error };
};
