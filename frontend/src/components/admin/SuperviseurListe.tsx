import { useState } from 'react';
import {
  CheckCircle2,
  XCircle,
  Loader2,
  AlertTriangle,
  Power,
  PowerOff,
  Clock,
  Eye,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import { useSuperviseurs, useSuperviseurActions } from '@/hooks/useSuperviseurs';
import type { SuperviseurListItemDTO } from '@/api/admin/superviseurs';

// ── Helpers ────────────────────────────────────────────────────────────────

function buildPages(current: number, total: number): (number | '…')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: (number | '…')[] = [1];
  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);
  if (start > 2) pages.push('…');
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < total - 1) pages.push('…');
  pages.push(total);
  return pages;
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
        'flex h-8 w-8 items-center justify-center rounded-xl transition-all',
        disabled
          ? 'cursor-not-allowed text-muted-foreground/40'
          : 'text-muted-foreground hover:bg-primary/10 hover:text-primary',
      )}
    >
      {children}
    </button>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="border-b border-border/60 px-5 py-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-[color:var(--danger)]/30 bg-[color:var(--danger-soft)] px-4 py-3">
      <AlertTriangle className="h-4 w-4 shrink-0 text-[color:var(--danger)]" />
      <p className="text-sm font-medium text-[color:var(--danger)]">{message}</p>
    </div>
  );
}

// ── Props ──────────────────────────────────────────────────────────────────

interface SuperviseurListeProps {
  onRowClick: (id: string) => void;
  onRefresh: () => void;
  onCreateNew: () => void;
  refreshKey?: number;
  activeDetailId?: string | null;
}

// ── Component ──────────────────────────────────────────────────────────────

export function SuperviseurListe({
  onRowClick,
  onRefresh,
  onCreateNew,
  refreshKey,
  activeDetailId,
}: SuperviseurListeProps) {
  const [filtreActif, setFiltreActif] = useState<boolean | undefined>(undefined);
  const [filtreCompteActive, setFiltreCompteActive] = useState<boolean | undefined>(undefined);
  const [page, setPage] = useState(1); // 1-based for display parity with history.tsx
  const [size] = useState(8);
  const [actionId, setActionId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // API uses 0-based page index
  const { data, loading, error } = useSuperviseurs({
    actif: filtreActif,
    compteActive: filtreCompteActive,
    page: page - 1,
    size,
    refreshKey,
  });
  const { activate, deactivate } = useSuperviseurActions();

  const handleToggleActif = async (item: SuperviseurListItemDTO) => {
    setActionId(item.id);
    setActionError(null);
    const success = item.actif ? await deactivate(item.id) : await activate(item.id);
    if (success) onRefresh();
    setActionId(null);
  };

  const superviseurs = data?.content || [];
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const currentPage = Math.min(page, totalPages);
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="neu-card p-6 h-full flex flex-col overflow-hidden">
      {/* ── Header ── */}
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold">Liste des superviseurs</h2>
        <button
          onClick={onCreateNew}
          className="flex items-center gap-2 rounded-2xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] transition-all hover:opacity-90"
        >
          + Nouveau superviseur
        </button>
      </div>

      {/* ── Filters ── */}
      <div className="mt-5">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Filtre
          </span>

          {/* neu-inset pill group – mirrors history.tsx mode toggle */}
          <div className="neu-inset flex gap-1 rounded-2xl p-1 h-10 items-center">
            {[
              {
                label: 'Tous',
                onClick: () => { setFiltreActif(undefined); setFiltreCompteActive(undefined); setPage(1); },
                active: filtreActif === undefined && filtreCompteActive === undefined,
              },
              {
                label: 'Actifs',
                onClick: () => { setFiltreActif(true); setFiltreCompteActive(undefined); setPage(1); },
                active: filtreActif === true && filtreCompteActive === undefined,
              },
              {
                label: 'Inactifs',
                onClick: () => { setFiltreActif(false); setFiltreCompteActive(undefined); setPage(1); },
                active: filtreActif === false && filtreCompteActive === undefined,
              },
              {
                label: 'En attente',
                onClick: () => { setFiltreActif(undefined); setFiltreCompteActive(false); setPage(1); },
                active: filtreCompteActive === false,
              },
            ].map((f) => (
              <button
                key={f.label}
                onClick={f.onClick}
                className={cn(
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all h-8 flex items-center',
                  f.active
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ── Error banner ── */}
      {actionError && (
        <div className="mt-4">
          <ErrorBanner message={actionError} />
        </div>
      )}

      {/* ── Table ── */}
      <div className="mt-6 flex-1 min-h-0 overflow-hidden rounded-2xl border border-border flex flex-col bg-card">
        <div className="flex-1 overflow-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="sticky top-0 z-10 bg-[color:var(--surface-raised)] backdrop-blur">
              <tr className="text-left">
                <Th>Superviseur</Th>
                <Th>Email</Th>
                <Th>Activation Initiale</Th>
                <Th>Détails</Th>
                <Th>Statut</Th>
              </tr>
            </thead>
            <tbody>
              {/* Loading */}
              {loading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center">
                    <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Chargement…
                    </div>
                  </td>
                </tr>
              )}

              {/* Error */}
              {!loading && error && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center">
                    <p className="text-sm text-[color:var(--danger)] flex items-center justify-center gap-2">
                      <AlertTriangle className="h-4 w-4" />
                      Impossible de charger la liste des superviseurs.
                    </p>
                  </td>
                </tr>
              )}

              {/* Empty */}
              {!loading && !error && superviseurs.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-sm text-muted-foreground">
                    Aucun superviseur trouvé pour ces filtres.
                  </td>
                </tr>
              )}

              {/* Rows */}
              {!loading &&
                !error &&
                superviseurs.map((item, i) => (
                  <SuperviseurRow
                    key={item.id}
                    item={item}
                    striped={i % 2 === 1}
                    isActive={activeDetailId === item.id}
                    isLoading={actionId === item.id}
                    onRowClick={onRowClick}
                    onToggle={handleToggleActif}
                  />
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Pagination – same layout as history.tsx ── */}
      <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-muted-foreground">
          Page {currentPage} sur {totalPages}
        </p>

        <div className="neu-inset flex items-center gap-1 rounded-2xl p-1">
          <PageBtn
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1 || loading}
          >
            <ChevronLeft className="h-4 w-4" />
          </PageBtn>

          {buildPages(currentPage, totalPages).map((p, i) =>
            p === '…' ? (
              <span key={i} className="px-2 text-xs text-muted-foreground">
                …
              </span>
            ) : (
              <button
                key={i}
                onClick={() => setPage(p as number)}
                className={cn(
                  'h-8 min-w-8 rounded-xl px-2 text-xs font-semibold transition-all',
                  currentPage === p
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                {p}
              </button>
            ),
          )}

          <PageBtn
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages || loading}
          >
            <ChevronRight className="h-4 w-4" />
          </PageBtn>
        </div>
      </div>
    </div>
  );
}

// ── Row sub-component ──────────────────────────────────────────────────────

function SuperviseurRow({
  item,
  striped,
  isActive,
  isLoading,
  onRowClick,
  onToggle,
}: {
  item: SuperviseurListItemDTO;
  striped: boolean;
  isActive: boolean;
  isLoading: boolean;
  onRowClick: (id: string) => void;
  onToggle: (item: SuperviseurListItemDTO) => void;
}) {
  return (
    <tr
      className={cn(
        'transition-colors',
        isActive
          ? 'bg-primary/8 ring-1 ring-inset ring-primary/20'
          : striped
          ? 'bg-[color:oklch(0.98_0.005_90)]'
          : 'bg-transparent',
        'hover:bg-primary/5',
      )}
    >
      {/* Superviseur */}
      <td className="px-5 py-3">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-xs font-bold text-primary">
            {(item.prenom?.[0] ?? '').toUpperCase()}
            {(item.nom?.[0] ?? '').toUpperCase()}
          </div>
          <span className="font-semibold">
            {item.prenom} {item.nom}
          </span>
        </div>
      </td>

      {/* Email */}
      <td className="px-5 py-3 text-sm text-muted-foreground">{item.email}</td>

      {/* Activation */}
      <td className="px-5 py-3">
        {item.compteActive ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-500/15 px-2.5 py-1 text-xs font-semibold text-blue-500">
            <CheckCircle2 className="h-3 w-3" />
            Activé
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/15 px-2.5 py-1 text-xs font-semibold text-amber-500">
            <Clock className="h-3 w-3" />
            En attente
          </span>
        )}
      </td>

      {/* Détails */}
      <td className="px-5 py-3">
        <button
          onClick={() => onRowClick(item.id)}
          className={cn(
            'inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all',
            isActive
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'bg-primary/10 text-primary hover:bg-primary/20',
          )}
        >
          <Eye className="h-3 w-3" />
          Voir
        </button>
      </td>

      {/* Toggle Actif */}
      <td className="px-5 py-3" onClick={(e) => e.stopPropagation()}>
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <div className="flex items-center gap-2 cursor-pointer">
              <Switch
                checked={item.actif}
                disabled={isLoading}
                aria-label={item.actif ? 'Désactiver' : 'Activer'}
                className="data-[state=checked]:bg-orange-600"
              />
              {isLoading && <Loader2 className="h-3 w-3 animate-spin text-muted-foreground" />}
            </div>
          </AlertDialogTrigger>
          <AlertDialogContent className="rounded-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle className="flex items-center gap-2">
                {item.actif ? (
                  <>
                    <PowerOff className="h-5 w-5 text-[color:var(--danger)]" />
                    Désactiver le compte
                  </>
                ) : (
                  <>
                    <Power className="h-5 w-5 text-primary" />
                    Activer le compte
                  </>
                )}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {item.actif
                  ? `Êtes-vous sûr de vouloir désactiver le compte de ${item.prenom} ${item.nom} ? L'utilisateur ne pourra plus se connecter.`
                  : `Êtes-vous sûr de vouloir activer le compte de ${item.prenom} ${item.nom} ?`}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className="rounded-xl">Annuler</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => onToggle(item)}
                className="rounded-xl bg-primary text-primary-foreground hover:opacity-90"
              >
                {item.actif ? 'Désactiver' : 'Activer'}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </td>
    </tr>
  );
}
