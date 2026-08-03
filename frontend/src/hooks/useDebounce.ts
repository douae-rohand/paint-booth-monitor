import { useEffect, useState } from 'react';

/**
 * Hook utilitaire pour debounce une valeur.
 * Utile pour les champs de recherche pour éviter trop d'appels API.
 * 
 * @param value La valeur à debouncer
 * @param delay Le délai en millisecondes (défaut: 500ms)
 * @returns La valeur debouncée
 */
export const useDebounce = <T>(value: T, delay: number = 500): T => {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
};
