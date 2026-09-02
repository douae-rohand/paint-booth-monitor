import { useState, useEffect, useCallback, useRef } from 'react';
import { getLogs, type LogAuditDTO, type ActionAudit } from '@/api/audit';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface AuditLogFilters {
  selectedActions: ActionAudit[];
  idSuperviseur: string | undefined;
  dateDebut: string | undefined;
  dateFin: string | undefined;
}

export interface UseAuditLogsReturn {
  logs: LogAuditDTO[];
  loading: boolean;
  error: string | null;
  page: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  filters: AuditLogFilters;
  setFilters: (filters: AuditLogFilters) => void;
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;
  refresh: () => void;
}

const DEFAULT_FILTERS: AuditLogFilters = {
  selectedActions: [],
  idSuperviseur: undefined,
  dateDebut: undefined,
  dateFin: undefined,
};

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * useAuditLogs — encapsule l'appel GET /api/admin/audit avec filtres et pagination.
 *
 * Contrat :
 *   - Paramètres : aucun (état interne)
 *   - Retour : { logs, loading, error, page, pageSize, totalPages, totalElements,
 *               filters, setFilters, setPage, setPageSize, refresh }
 *   - setFilters() remet automatiquement page à 0
 *   - setPageSize() remet automatiquement page à 0
 *   - Toutes les actions sélectionnées sont transmises au backend en liste (filtre IN).
 *     Liste vide = pas de filtre action → retourne tout.
 */
export function useAuditLogs(): UseAuditLogsReturn {
  const [logs, setLogs] = useState<LogAuditDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPageState] = useState(0);
  const [pageSize, setPageSizeState] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFiltersState] = useState<AuditLogFilters>(DEFAULT_FILTERS);

  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    return () => { isMounted.current = false; };
  }, []);

  const fetchLogs = useCallback(async () => {
    if (!isMounted.current) return;
    setLoading(true);
    setError(null);

    try {
      // Transmettre toutes les actions sélectionnées au backend (liste = filtre IN)
      // Liste vide → pas de paramètre → backend retourne tout
      const data = await getLogs({
        idSuperviseur: filters.idSuperviseur,
        actions: filters.selectedActions.length > 0 ? filters.selectedActions : undefined,
        dateDebut: filters.dateDebut,
        dateFin: filters.dateFin,
        page,
        size: pageSize,
        // Pas de sort via Pageable — ORDER BY date_action DESC est hardcodé dans la requête native
      });

      const content = data.content ?? [];

      if (isMounted.current) {
        setLogs(content);
        setTotalPages(data.page?.totalPages ?? 0);
        setTotalElements(data.page?.totalElements ?? 0);
      }
    } catch (e) {
      console.error('[useAuditLogs] Erreur fetch logs:', e);
      if (isMounted.current) setError('Impossible de charger les logs d\'audit.');
    } finally {
      if (isMounted.current) setLoading(false);
    }
  }, [filters, page, pageSize]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  // setFilters remet page à 0
  const setFilters = useCallback((newFilters: AuditLogFilters) => {
    setFiltersState(newFilters);
    setPageState(0);
  }, []);

  // setPage direct
  const setPage = useCallback((newPage: number) => {
    setPageState(newPage);
  }, []);

  // setPageSize remet page à 0
  const setPageSize = useCallback((newSize: number) => {
    setPageSizeState(newSize);
    setPageState(0);
  }, []);

  const refresh = useCallback(() => {
    fetchLogs();
  }, [fetchLogs]);

  return {
    logs,
    loading,
    error,
    page,
    pageSize,
    totalPages,
    totalElements,
    filters,
    setFilters,
    setPage,
    setPageSize,
    refresh,
  };
}
