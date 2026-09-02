import { useState, useEffect, useCallback } from 'react';
import { getHistoriqueAlertes, type AlerteDTO, type AlertesParams, type AlertesPage } from '@/api/alerting';
import { getPointMesures, type PointMesureResponse } from '@/api/measures';
import { Calendar as CalendarIcon, Clock, ChevronLeft, ChevronRight, AlertTriangle, Loader2 } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import type { DateRange } from 'react-day-picker';

export function AlertesHistoryTable() {
  const [alertes, setAlertes] = useState<AlerteDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [statut, setStatut] = useState<string>('');
  const [typeAlerte, setTypeAlerte] = useState<string>('');
  const [severite, setSeverite] = useState<string>('');
  const [idPointMesure, setIdPointMesure] = useState<number | undefined>();
  const [dateMode, setDateMode] = useState<'exact' | 'range'>('range');
  const [exactDate, setExactDate] = useState<Date | undefined>();
  const [dateRange, setDateRange] = useState<DateRange | undefined>();

  const [pointMesures, setPointMesures] = useState<PointMesureResponse[]>([]);

  // Load point measures on mount
  useEffect(() => {
    const fetchPointMesures = async () => {
      try {
        const data = await getPointMesures();
        setPointMesures(data);
      } catch (e) {
        console.error('Erreur fetch point mesures:', e);
      }
    };
    fetchPointMesures();
  }, []);

  const fetchAlertes = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params: AlertesParams = {
        statut: statut || undefined,
        typeAlerte: typeAlerte || undefined,
        severite: severite || undefined,
        idPointMesure,
        page,
        size: pageSize,
      };

      if (dateMode === 'exact' && exactDate) {
        const startOfDay = new Date(exactDate);
        startOfDay.setHours(0, 0, 0, 0);
        const endOfDay = new Date(exactDate);
        endOfDay.setHours(23, 59, 59, 999);
        params.dateDebut = startOfDay.toISOString();
        params.dateFin = endOfDay.toISOString();
      } else if (dateMode === 'range' && dateRange?.from) {
        const startOfDay = new Date(dateRange.from);
        startOfDay.setHours(0, 0, 0, 0);
        params.dateDebut = startOfDay.toISOString();
        if (dateRange.to) {
          const endOfDay = new Date(dateRange.to);
          endOfDay.setHours(23, 59, 59, 999);
          params.dateFin = endOfDay.toISOString();
        }
      }

      const data: AlertesPage = await getHistoriqueAlertes(params);
      setAlertes(data.content);
      setTotalPages(data.page?.totalPages ?? 0);
      setTotalElements(data.page?.totalElements ?? 0);
    } catch (e) {
      console.error('Erreur fetch alertes:', e);
      setError('Impossible de charger l\'historique des alertes');
    } finally {
      setLoading(false);
    }
  }, [statut, typeAlerte, severite, idPointMesure, dateMode, exactDate, dateRange, page, pageSize]);

  useEffect(() => {
    fetchAlertes();
  }, [fetchAlertes]);

  // Reset page on filter change
  useEffect(() => {
    setPage(0);
  }, [statut, typeAlerte, severite, idPointMesure, dateMode, exactDate, dateRange, pageSize]);

  const formatDuree = (minutes: number): string => {
    if (minutes < 60) {
      return `${minutes} min`;
    }
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    
    if (hours >= 24) {
      const days = Math.floor(hours / 24);
      const remainingHours = hours % 24;
      const parts: string[] = [`${days}j`];
      if (remainingHours > 0) {
        parts.push(`${remainingHours}h`);
      }
      if (remainingMinutes > 0) {
        parts.push(`${remainingMinutes}min`);
      }
      return parts.join(' ');
    }

    if (remainingMinutes === 0) {
      return `${hours}h`;
    }
    return `${hours}h ${remainingMinutes}min`;
  };

  const formatAlertDateOnly = (dateStr: string): string => {
    if (!dateStr) return "—";
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return "—";
      return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
    } catch (e) {
      return "—";
    }
  };

  const formatHeureAvecMicrosecondes = (dateStr: string): string => {
    if (!dateStr) return "—";
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return "—";
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      let microseconds = "000000";
      const dotIndex = dateStr.indexOf('.');
      if (dotIndex !== -1) {
        const afterDot = dateStr.substring(dotIndex + 1);
        const digitMatch = afterDot.match(/^(\d+)/);
        if (digitMatch) {
          microseconds = digitMatch[1].padEnd(6, '0').substring(0, 6);
        }
      }
      return `${hours}:${minutes}:${seconds}.${microseconds}`;
    } catch (e) {
      return "—";
    }
  };

  const getStatutBadgeClass = (statut: string): string => {
    if (statut === 'ACTIVE') {
      return 'bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300 border border-rose-200/60 dark:border-rose-900/40 font-semibold shadow-none';
    }
    return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300 border border-emerald-200/60 dark:border-emerald-900/40 font-semibold shadow-none';
  };

  const getSeverityBadgeClass = (severite: string): string => {
    switch (severite) {
      case 'CRITIQUE':
        return 'bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300 border border-rose-200/60 dark:border-rose-900/40 font-semibold shadow-none';
      case 'MOYENNE':
        return 'bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300 border border-amber-200/60 dark:border-amber-900/40 font-semibold shadow-none';
      default:
        return 'bg-slate-50 text-slate-700 dark:bg-slate-950/30 dark:text-slate-300 border border-slate-200/60 dark:border-slate-800/40 font-semibold shadow-none';
    }
  };

  const resetDateFilter = () => {
    setExactDate(undefined);
    setDateRange(undefined);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setPage(0);
  };

  if (error) {
    return (
      <div className="neu-card p-6">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  const current = Math.min(page, totalPages - 1);

  return (
    <div className="flex flex-col min-h-full gap-5 animate-in fade-in duration-300">
      {/* Filters */}
      <div className="shrink-0">
        <div className="flex flex-wrap items-center gap-2">
          {/* Statut */}
          <div className="neu-inset rounded-2xl px-1">
            <Select value={statut} onValueChange={setStatut}>
              <SelectTrigger className="h-10 w-[120px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue placeholder="Statut" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Tous</SelectItem>
                <SelectItem value="ACTIVE">Actives</SelectItem>
                <SelectItem value="RESOLUE">Résolues</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Type */}
          <div className="neu-inset rounded-2xl px-1">
            <Select value={typeAlerte} onValueChange={setTypeAlerte}>
              <SelectTrigger className="h-10 w-[120px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Tous</SelectItem>
                <SelectItem value="SEUIL_ABSOLU">Seuil absolu</SelectItem>
                <SelectItem value="SEUIL_DYNAMIQUE">Seuil dynamique</SelectItem>
                <SelectItem value="DERIVE_IA">Dérive IA</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Sévérité */}
          <div className="neu-inset rounded-2xl px-1">
            <Select value={severite} onValueChange={setSeverite}>
              <SelectTrigger className="h-10 w-[120px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue placeholder="Sévérité" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Toutes</SelectItem>
                <SelectItem value="CRITIQUE">Critique</SelectItem>
                <SelectItem value="MOYENNE">Moyenne</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Point de mesure */}
          <div className="neu-inset rounded-2xl px-1">
            <Select value={idPointMesure?.toString()} onValueChange={(v) => setIdPointMesure(v ? Number(v) : undefined)}>
              <SelectTrigger className="h-10 w-[160px] border-0 bg-transparent shadow-none focus:ring-0">
                <SelectValue placeholder="Point de mesure" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Tous</SelectItem>
                {pointMesures.map((pm) => (
                  <SelectItem key={pm.id} value={pm.id.toString()}>
                    {pm.nom}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Date mode + picker — grouped on one line */}
          <div className="flex items-center gap-2">
            <div className="neu-inset flex gap-1 rounded-2xl p-1 h-10 items-center">
              <button
                onClick={() => setDateMode('exact')}
                className={cn(
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center',
                  dateMode === 'exact'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                Date exacte
              </button>
              <button
                onClick={() => setDateMode('range')}
                className={cn(
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center',
                  dateMode === 'range'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                Plage
              </button>
            </div>

            <Popover>
              <PopoverTrigger asChild>
                <button className="neu-pressable flex h-10 items-center gap-2 rounded-2xl px-4 text-sm bg-[color:var(--surface)] border border-border/50 hover:bg-muted/40 transition-colors whitespace-nowrap">
                  <CalendarIcon className="h-4 w-4 text-primary shrink-0" />
                  {dateMode === 'exact'
                    ? exactDate
                      ? format(exactDate, 'd MMM yyyy', { locale: fr })
                      : 'Choisir une date'
                    : dateRange?.from
                      ? dateRange.to
                        ? `${format(dateRange.from, 'd MMM', { locale: fr })} - ${format(dateRange.to, 'd MMM yyyy', { locale: fr })}`
                        : format(dateRange.from, 'd MMM yyyy', { locale: fr })
                      : 'Choisir une plage'}
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                {dateMode === 'exact' ? (
                  <Calendar
                    mode="single"
                    selected={exactDate}
                    onSelect={setExactDate}
                    locale={fr}
                    className={cn('p-3 pointer-events-auto')}
                  />
                ) : (
                  <Calendar
                    mode="range"
                    selected={dateRange}
                    onSelect={setDateRange}
                    locale={fr}
                    numberOfMonths={2}
                    className={cn('p-3 pointer-events-auto')}
                  />
                )}
              </PopoverContent>
            </Popover>

            {(exactDate || dateRange) && (
              <button
                onClick={resetDateFilter}
                className="text-xs font-semibold text-muted-foreground underline-offset-4 hover:underline"
              >
                Réinitialiser
              </button>
            )}
          </div>
        </div>
      </div>


      {/* Table */}
      <div className="flex flex-col">
        <div className="min-h-[400px] overflow-hidden rounded-2xl border border-border/60 bg-[color:var(--surface)]">
          <div className="h-full overflow-auto">
            <table className="w-full border-collapse text-sm">
              <thead className="sticky top-0 z-10 bg-[color:var(--surface-raised)] backdrop-blur-sm border-b border-border/60">
                <tr className="text-left">
                  <Th>Date</Th>
                  <Th>Heure</Th>
                  <Th>Point de mesure</Th>
                  <Th>Métrique</Th>
                  <Th>Type</Th>
                  <Th>Sévérité</Th>
                  <Th>Statut</Th>
                  <Th>Durée</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {loading && alertes.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-6 py-16 text-center text-sm text-muted-foreground">
                      <div className="flex flex-col items-center gap-2">
                        <Loader2 className="h-6 w-6 animate-spin text-primary" />
                        <span>Chargement...</span>
                      </div>
                    </td>
                  </tr>
                ) : alertes.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-6 py-16 text-center text-sm text-muted-foreground">
                      <div className="flex flex-col items-center gap-2">
                        <AlertTriangle className="h-6 w-6 text-muted-foreground/40" />
                        <span>Aucune alerte trouvée pour les filtres sélectionnés</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  alertes.map((alerte, i) => (
                    <tr
                      key={alerte.idAlerte}
                      className={cn(
                        "transition-colors hover:bg-primary/5",
                        i % 2 === 1 ? "bg-[color:var(--surface-muted)]/30" : "bg-transparent",
                      )}
                    >
                      <td className="px-5 py-3.5 font-medium text-foreground">
                        {formatAlertDateOnly(alerte.dateCreation)}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground">
                        {formatHeureAvecMicrosecondes(alerte.dateCreation)}
                      </td>
                      <td className="px-5 py-3.5 font-semibold text-foreground">
                        {alerte.pointMesureNom}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground">
                        {alerte.metrique}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground">
                        {alerte.typeAlerte}
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge className={getSeverityBadgeClass(alerte.severite)}>
                          {alerte.severite === 'CRITIQUE' ? 'Critique' : alerte.severite === 'MOYENNE' ? 'Moyenne' : alerte.severite}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge className={getStatutBadgeClass(alerte.statut)}>
                          {alerte.statut === 'ACTIVE' ? 'Active' : 'Résolue'}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground">
                        <span className="inline-flex items-center gap-1">
                          {formatDuree(alerte.dureeMinutes)}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 0 && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 px-1">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span className="font-medium">Lignes par page</span>
            <div className="neu-inset rounded-xl">
              <Select value={String(pageSize)} onValueChange={(v) => handlePageSizeChange(Number(v))}>
                <SelectTrigger className="h-9 w-[80px] border-0 bg-transparent shadow-none focus:ring-0">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[10, 25, 50].map((s) => (
                    <SelectItem key={s} value={String(s)}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <span className="ml-2 font-medium">
              Page {current + 1} <span className="text-muted-foreground/60">sur</span> {totalPages}
            </span>
          </div>

          <div className="neu-inset flex items-center gap-1 rounded-2xl p-1.5">
            <PageBtn onClick={() => handlePageChange(Math.max(0, current - 1))} disabled={current === 0}>
              <ChevronLeft className="h-4 w-4" />
            </PageBtn>
            {buildPages(current + 1, totalPages).map((p, i) =>
              p === "…" ? (
                <span key={i} className="px-2 text-xs text-muted-foreground/60">…</span>
              ) : (
                <button
                  key={i}
                  onClick={() => handlePageChange((p as number) - 1)}
                  className={cn(
                    "h-8 min-w-8 rounded-xl px-2 text-xs font-semibold transition-all",
                    current + 1 === p
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-muted-foreground hover:bg-primary/10 hover:text-foreground",
                  )}
                >
                  {p}
                </button>
              )
            )}
            <PageBtn onClick={() => handlePageChange(Math.min(totalPages - 1, current + 1))} disabled={current === totalPages - 1}>
              <ChevronRight className="h-4 w-4" />
            </PageBtn>
          </div>
        </div>
      )}
    </div>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="border-b border-border/60 px-5 py-3.5 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

function PageBtn({
  children,
  onClick,
  disabled,
}: {
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex h-8 w-8 items-center justify-center rounded-xl transition-all",
        disabled
          ? "cursor-not-allowed text-muted-foreground/40"
          : "text-muted-foreground hover:bg-primary/10 hover:text-primary",
      )}
    >
      {children}
    </button>
  );
}

function buildPages(current: number, total: number): (number | "…")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: (number | "…")[] = [1];
  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);
  if (start > 2) pages.push("…");
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < total - 1) pages.push("…");
  pages.push(total);
  return pages;
}
