/**
 * useMeasures – real-time and historical temperature/humidity data
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { getHistoriqueCabine, getHistoriqueEtuve } from '../api/measures/index';
import type { HistoriqueCabineParams, HistoriqueEtuveParams, Page, MesureCabineDTO, MesureEtuveDTO } from '../api/measures/index';

/**
 * Hook pour l'historique des mesures de la cabine.
 * Gère la pagination et les filtres (date, seulement dépassements).
 */
export const useHistoriqueCabine = (params: HistoriqueCabineParams) => {
  const [data, setData] = useState<Page<MesureCabineDTO> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const paramsRef = useRef(params);

  // Mettre à jour la ref quand params change
  useEffect(() => {
    paramsRef.current = params;
  }, [params]);

  const fetch = useCallback(async () => {
    // Ne pas faire l'appel si size est 0 (indique que l'onglet n'est pas actif)
    if (paramsRef.current.size === 0) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const result = await getHistoriqueCabine(paramsRef.current);
      setData(result);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Re-fetch automatique à chaque changement de page, taille ou filtre.
  // On dépend des valeurs primitives de params, pas de l'objet entier (évite
  // les boucles infinies dues à la re-création de l'objet à chaque render).
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetch();
  }, [
    params.page,
    params.size,
    params.dateDebut,
    params.dateFin,
    params.seulementDepassements,
  ]);

  return { data, loading, error, refetch: fetch };
};

/**
 * Hook pour l'historique des mesures de l'étuve.
 * Gère la pagination et les filtres (zone, date, seulement dépassements).
 */
export const useHistoriqueEtuve = (params: HistoriqueEtuveParams) => {
  const [data, setData] = useState<Page<MesureEtuveDTO> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const paramsRef = useRef(params);

  // Mettre à jour la ref quand params change
  useEffect(() => {
    paramsRef.current = params;
  }, [params]);

  const fetch = useCallback(async () => {
    // Ne pas faire l'appel si size est 0 (indique que l'onglet n'est pas actif)
    if (paramsRef.current.size === 0) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const result = await getHistoriqueEtuve(paramsRef.current);
      setData(result);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Re-fetch automatique à chaque changement de page, taille, zone ou filtre.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetch();
  }, [
    params.page,
    params.size,
    params.dateDebut,
    params.dateFin,
    params.seulementDepassements,
    params.zone,
  ]);

  return { data, loading, error, refetch: fetch };
};
