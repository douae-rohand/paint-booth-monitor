/**
 * useSuperviseurs – superviseur list and management
 */
import { useState, useEffect } from 'react';
import {
  listSuperviseurs,
  getSuperviseur,
  createSuperviseur,
  updateSuperviseur,
  activerSuperviseur,
  desactiverSuperviseur,
  renvoyerActivationSuperviseur,
  type SuperviseurListItemDTO,
  type SuperviseurResponseDTO,
  type SuperviseurCreateDTO,
  type SuperviseurUpdateDTO,
  type SuperviseurPageResponse,
} from '../api/admin/superviseurs';

export const useSuperviseurs = (params?: { actif?: boolean; compteActive?: boolean; page?: number; size?: number; refreshKey?: number }) => {
  const [data, setData] = useState<SuperviseurPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await listSuperviseurs(params);
        setData(result);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [params?.actif, params?.compteActive, params?.page, params?.size, params?.refreshKey]);

  return { data, loading, error };
};

export const useSuperviseur = (id: string, refreshKey?: number) => {
  const [data, setData] = useState<SuperviseurResponseDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getSuperviseur(id);
        setData(result);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [id, refreshKey]);

  return { data, loading, error };
};

export const useSuperviseurActions = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = async (data: SuperviseurCreateDTO): Promise<SuperviseurResponseDTO | null> => {
    setLoading(true);
    setError(null);
    try {
      const result = await createSuperviseur(data);
      return result;
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message || 'Erreur lors de la création');
      return null;
    } finally {
      setLoading(false);
    }
  };

  const update = async (id: string, data: SuperviseurUpdateDTO): Promise<SuperviseurResponseDTO | null> => {
    setLoading(true);
    setError(null);
    try {
      const result = await updateSuperviseur(id, data);
      return result;
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message || 'Erreur lors de la modification');
      return null;
    } finally {
      setLoading(false);
    }
  };

  const activate = async (id: string): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      await activerSuperviseur(id);
      return true;
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message || 'Erreur lors de l\'activation');
      return false;
    } finally {
      setLoading(false);
    }
  };

  const deactivate = async (id: string): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      await desactiverSuperviseur(id);
      return true;
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message || 'Erreur lors de la désactivation');
      return false;
    } finally {
      setLoading(false);
    }
  };

  const resendActivation = async (id: string): Promise<SuperviseurResponseDTO | null> => {
    setLoading(true);
    setError(null);
    try {
      const result = await renvoyerActivationSuperviseur(id);
      return result;
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message || 'Erreur lors du renvoi du lien d\'activation');
      return null;
    } finally {
      setLoading(false);
    }
  };

  return { create, update, activate, deactivate, resendActivation, loading, error };
};
