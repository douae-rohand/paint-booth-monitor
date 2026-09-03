import { useEffect, useState } from 'react';
import { X, CalendarIcon, RotateCcw } from 'lucide-react';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { cn } from '@/lib/utils';
import {
  AUDIT_CATEGORIE_GROUPES,
  AUDIT_ACTION_META,
  AUDIT_CATEGORIE_COLORS,
} from '@/constants/auditLabels';
import type { ActionAudit } from '@/api/audit';
import type { SuperviseurListItemDTO } from '@/api/admin/superviseurs';
import type { DateRange } from 'react-day-picker';

// ── Périodes rapides ──────────────────────────────────────────────────────────

const PERIODES = [
  { key: 'today',  label: "Aujourd'hui", days: 0 },
  { key: '7j',     label: '7 jours',     days: 7 },
  { key: '30j',    label: '30 jours',    days: 30 },
] as const;

type PeriodeKey = (typeof PERIODES)[number]['key'] | 'custom' | '';

// ── Props ─────────────────────────────────────────────────────────────────────

export interface AuditFilters {
  selectedActions: ActionAudit[];
  idSuperviseur: string | undefined;
  dateDebut: string | undefined;
  dateFin: string | undefined;
}

interface AuditFiltersSectionProps {
  filters: AuditFilters;
  onChange: (filters: AuditFilters) => void;
  totalElements: number;
  superviseurs: SuperviseurListItemDTO[];
}

// ── Composant ─────────────────────────────────────────────────────────────────

export function AuditFiltersSection({
  filters,
  onChange,
  totalElements,
  superviseurs,
}: AuditFiltersSectionProps) {
  const [periode, setPeriode] = useState<PeriodeKey>('');
  const [customRange, setCustomRange] = useState<DateRange | undefined>();
  const [superviseurSearch, setSuperviseurSearch] = useState('');
  const [superviseurOpen, setSuperviseurOpen] = useState(false);

  // Calcul des dates selon la période rapide
  const applyPeriode = (key: PeriodeKey) => {
    setPeriode(key);
    if (key === '' || key === 'custom') return;

    const now = new Date();
    now.setHours(23, 59, 59, 999);
    const periodeObj = PERIODES.find((p) => p.key === key);
    if (!periodeObj) return;

    if (periodeObj.days === 0) {
      const start = new Date();
      start.setHours(0, 0, 0, 0);
      onChange({ ...filters, dateDebut: start.toISOString(), dateFin: now.toISOString() });
    } else {
      const start = new Date(now);
      start.setDate(start.getDate() - periodeObj.days);
      start.setHours(0, 0, 0, 0);
      onChange({ ...filters, dateDebut: start.toISOString(), dateFin: now.toISOString() });
    }
  };

  // Mise à jour de la plage personnalisée
  useEffect(() => {
    if (periode !== 'custom') return;
    if (!customRange?.from) {
      onChange({ ...filters, dateDebut: undefined, dateFin: undefined });
      return;
    }
    const start = new Date(customRange.from);
    start.setHours(0, 0, 0, 0);
    const end = customRange.to ? new Date(customRange.to) : new Date(customRange.from);
    end.setHours(23, 59, 59, 999);
    onChange({ ...filters, dateDebut: start.toISOString(), dateFin: end.toISOString() });
  }, [customRange, periode]);

  // Toggle d'une action dans la sélection multiple
  const toggleAction = (action: ActionAudit) => {
    const already = filters.selectedActions.includes(action);
    onChange({
      ...filters,
      selectedActions: already
        ? filters.selectedActions.filter((a) => a !== action)
        : [...filters.selectedActions, action],
    });
  };

  // Toggle d'un groupe entier
  const toggleGroupe = (actions: ActionAudit[]) => {
    const allSelected = actions.every((a) => filters.selectedActions.includes(a));
    if (allSelected) {
      onChange({
        ...filters,
        selectedActions: filters.selectedActions.filter((a) => !actions.includes(a)),
      });
    } else {
      const toAdd = actions.filter((a) => !filters.selectedActions.includes(a));
      onChange({ ...filters, selectedActions: [...filters.selectedActions, ...toAdd] });
    }
  };

  // Réinitialiser tout
  const reset = () => {
    setPeriode('');
    setCustomRange(undefined);
    setSuperviseurSearch('');
    onChange({ selectedActions: [], idSuperviseur: undefined, dateDebut: undefined, dateFin: undefined });
  };

  const hasFilters =
    filters.selectedActions.length > 0 ||
    filters.idSuperviseur !== undefined ||
    filters.dateDebut !== undefined;

  const superviseurSelectionne = superviseurs.find((s) => s.id === filters.idSuperviseur);

  const superviseursFiltres = superviseurs
    .filter((s) => {
      const q = superviseurSearch.trim().toLowerCase();
      if (!q) return true;
      return (s.email || '').toLowerCase().includes(q);
    })
    .sort((a, b) => {
      const q = superviseurSearch.trim().toLowerCase();
      if (!q) return 0;

      const aStarts = (a.email || '').toLowerCase().startsWith(q);
      const bStarts = (b.email || '').toLowerCase().startsWith(q);

      if (aStarts && !bStarts) return -1;
      if (!aStarts && bStarts) return 1;
      return 0;
    });

  return (
    <div className="neu-card p-5 space-y-5">
      {/* En-tête */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Filtres
          </span>
          <span className="text-xs font-semibold text-foreground">
            {totalElements > 0
              ? `${totalElements.toLocaleString('fr-FR')} événement${totalElements > 1 ? 's' : ''} trouvé${totalElements > 1 ? 's' : ''}`
              : 'Aucun événement'}
          </span>
        </div>
        {hasFilters && (
          <button
            onClick={reset}
            className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground transition-colors"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Réinitialiser
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        {/* ── Filtre actions (multi-choix groupés) ── */}
        <div className="lg:col-span-2 space-y-3">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Type d'action
          </p>
          <div className="space-y-3">
            {AUDIT_CATEGORIE_GROUPES.map((groupe) => {
              const allSelected = groupe.actions.every((a) =>
                filters.selectedActions.includes(a),
              );
              const someSelected = groupe.actions.some((a) =>
                filters.selectedActions.includes(a),
              );
              return (
                <div key={groupe.label} className="space-y-1.5">
                  {/* Label groupe cliquable */}
                  <button
                    onClick={() => toggleGroupe(groupe.actions)}
                    className={cn(
                      'text-xs font-bold uppercase tracking-wider transition-colors',
                      allSelected
                        ? 'text-primary'
                        : someSelected
                          ? 'text-foreground'
                          : 'text-muted-foreground hover:text-foreground',
                    )}
                  >
                    {groupe.label}
                    {someSelected && !allSelected && (
                      <span className="ml-1 text-primary">
                        ({filters.selectedActions.filter((a) => groupe.actions.includes(a)).length}
                        /{groupe.actions.length})
                      </span>
                    )}
                  </button>
                  {/* Pills des actions */}
                  <div className="flex flex-wrap gap-1.5">
                    {groupe.actions.map((action) => {
                      const meta = AUDIT_ACTION_META[action];
                      const selected = filters.selectedActions.includes(action);
                      const colorClass = AUDIT_CATEGORIE_COLORS[meta.categorie];
                      return (
                        <button
                          key={action}
                          onClick={() => toggleAction(action)}
                          className={cn(
                            'inline-flex items-center gap-1 rounded-xl border px-2.5 py-1 text-xs font-semibold transition-all',
                            selected
                              ? cn(colorClass, 'opacity-100 shadow-sm')
                              : 'bg-muted/40 text-muted-foreground border-border/50 hover:bg-muted hover:text-foreground',
                          )}
                        >
                          {selected && <span className="h-1.5 w-1.5 rounded-full bg-current" />}
                          {meta.label}
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* ── Colonne droite : utilisateur (par email) + période ── */}
        <div className="space-y-5">
          {/* Filtre utilisateur par email avec autocomplete */}
          <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Email de l'utilisateur
            </p>
            <div className="relative">
              {superviseurSelectionne ? (
                <div className="neu-inset flex items-center justify-between rounded-2xl px-3 py-2">
                  <div className="flex flex-col truncate">
                    <span className="text-sm font-semibold text-foreground truncate">
                      {superviseurSelectionne.email}
                    </span>
                    <span className="text-xs text-muted-foreground truncate">
                      {superviseurSelectionne.prenom} {superviseurSelectionne.nom}
                    </span>
                  </div>
                  <button
                    onClick={() => {
                      onChange({ ...filters, idSuperviseur: undefined });
                      setSuperviseurSearch('');
                    }}
                    className="ml-2 shrink-0 text-muted-foreground hover:text-foreground"
                    title="Effacer le filtre"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              ) : (
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Rechercher par email..."
                    value={superviseurSearch}
                    onChange={(e) => {
                      setSuperviseurSearch(e.target.value);
                      setSuperviseurOpen(true);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && superviseursFiltres.length > 0) {
                        e.preventDefault();
                        const s = superviseursFiltres[0];
                        onChange({ ...filters, idSuperviseur: s.id });
                        setSuperviseurSearch('');
                        setSuperviseurOpen(false);
                      }
                    }}
                    onFocus={() => setSuperviseurOpen(true)}
                    onBlur={() => setTimeout(() => setSuperviseurOpen(false), 200)}
                    className="neu-inset w-full rounded-2xl px-3 py-2 text-sm placeholder:text-muted-foreground bg-transparent outline-none border-0 focus:ring-0"
                  />
                  {superviseurOpen && (
                    <div className="absolute top-full left-0 z-50 mt-1 w-full rounded-2xl border border-border bg-[color:var(--surface)] shadow-lg overflow-hidden max-h-48 overflow-y-auto">
                      {superviseursFiltres.length > 0 ? (
                        superviseursFiltres.map((s) => (
                          <button
                            key={s.id}
                            type="button"
                            onMouseDown={() => {
                              onChange({ ...filters, idSuperviseur: s.id });
                              setSuperviseurSearch('');
                              setSuperviseurOpen(false);
                            }}
                            className="w-full px-3 py-2 text-left text-sm hover:bg-primary/10 transition-colors flex justify-between items-center"
                          >
                            <span className="font-semibold text-foreground truncate">
                              {s.email}
                            </span>
                            <span className="ml-2 text-xs text-muted-foreground shrink-0">
                              {s.prenom} {s.nom}
                            </span>
                          </button>
                        ))
                      ) : (
                        <div className="px-3 py-3 text-xs text-muted-foreground text-center">
                          Aucun utilisateur trouvé avec cet email
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Filtre période */}
          <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Période
            </p>
            {/* Raccourcis */}
            <div className="neu-inset flex gap-1 rounded-2xl p-1">
              <button
                onClick={() => applyPeriode('')}
                className={cn(
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all',
                  periode === ''
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                Tout
              </button>
              {PERIODES.map((p) => (
                <button
                  key={p.key}
                  onClick={() => applyPeriode(p.key)}
                  className={cn(
                    'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all',
                    periode === p.key
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground',
                  )}
                >
                  {p.label}
                </button>
              ))}
            </div>
            {/* Plage personnalisée */}
            <Popover>
              <PopoverTrigger asChild>
                <button
                  onClick={() => setPeriode('custom')}
                  className={cn(
                    'neu-pressable flex w-full items-center gap-2 rounded-2xl px-3 py-2 text-xs font-semibold transition-all border border-border/50',
                    periode === 'custom'
                      ? 'bg-primary/10 text-primary border-primary/30'
                      : 'text-muted-foreground hover:text-foreground',
                  )}
                >
                  <CalendarIcon className="h-3.5 w-3.5 shrink-0" />
                  {periode === 'custom' && customRange?.from
                    ? customRange.to
                      ? `${format(customRange.from, 'd MMM', { locale: fr })} – ${format(customRange.to, 'd MMM yyyy', { locale: fr })}`
                      : format(customRange.from, 'd MMM yyyy', { locale: fr })
                    : 'Plage personnalisée'}
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="end">
                <Calendar
                  mode="range"
                  selected={customRange}
                  onSelect={(r) => {
                    setCustomRange(r);
                    if (r?.from) setPeriode('custom');
                  }}
                  locale={fr}
                  numberOfMonths={1}
                  className="p-3 pointer-events-auto"
                  modifiers={{ today: undefined }}
                />
              </PopoverContent>
            </Popover>
          </div>
        </div>
      </div>
    </div>
  );
}
