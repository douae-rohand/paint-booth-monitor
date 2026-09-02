import { ChevronLeft, ChevronRight, Loader2, ShieldAlert, User } from 'lucide-react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import { ActionBadge } from './ActionBadge';
import type { LogAuditDTO } from '@/api/audit';

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatDate(dateStr: string): string {
  try {
    return format(new Date(dateStr), 'd MMM yyyy', { locale: fr });
  } catch {
    return '—';
  }
}

function formatTime(dateStr: string): string {
  try {
    return format(new Date(dateStr), 'HH:mm:ss', { locale: fr });
  } catch {
    return '—';
  }
}

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

// ── Sous-composants ───────────────────────────────────────────────────────────

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="px-4 py-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  );
}

// ── Props ─────────────────────────────────────────────────────────────────────

interface AuditTableProps {
  logs: LogAuditDTO[];
  loading: boolean;
  error: string | null;
  page: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}

// ── Composant ─────────────────────────────────────────────────────────────────

export function AuditTable({
  logs,
  loading,
  error,
  page,
  pageSize,
  totalPages,
  totalElements,
  onPageChange,
  onPageSizeChange,
}: AuditTableProps) {
  const current = Math.min(page, Math.max(0, totalPages - 1));

  return (
    <div className="neu-card p-6 flex flex-col gap-5">
      {/* Tableau */}
      <div className="overflow-hidden rounded-2xl border border-border/60 bg-[color:var(--surface)]">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-[color:var(--surface-raised)] border-b border-border/60">
              <tr>
                <Th>Date</Th>
                <Th>Heure</Th>
                <Th>Utilisateur</Th>
                <Th>Email</Th>
                <Th>Action</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40">
              {/* État chargement */}
              {loading && logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-14 text-center text-sm text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <Loader2 className="h-6 w-6 animate-spin text-primary" />
                      <span>Chargement des logs d'audit…</span>
                    </div>
                  </td>
                </tr>
              )}

              {/* Erreur réseau */}
              {!loading && error && (
                <tr>
                  <td colSpan={5} className="px-4 py-14 text-center text-sm">
                    <div className="flex flex-col items-center gap-2">
                      <ShieldAlert className="h-7 w-7 text-destructive/50" />
                      <span className="font-semibold text-destructive">{error}</span>
                    </div>
                  </td>
                </tr>
              )}

              {/* Aucun résultat (pas d'erreur) */}
              {!loading && !error && logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-14 text-center text-sm text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <ShieldAlert className="h-7 w-7 text-muted-foreground/30" />
                      <span className="font-semibold text-foreground">
                        Aucun événement ne correspond aux filtres sélectionnés
                      </span>
                      <span className="text-xs">Modifiez les filtres pour élargir la recherche.</span>
                    </div>
                  </td>
                </tr>
              )}

              {/* Données */}
              {!error &&
                logs.map((log, i) => (
                  <tr
                    key={log.idLog}
                    className={cn(
                      'transition-colors hover:bg-primary/5',
                      i % 2 === 1 ? 'bg-[color:var(--surface-muted)]/30' : 'bg-transparent',
                    )}
                  >
                    {/* Date */}
                    <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                      {formatDate(log.dateAction)}
                    </td>

                    {/* Heure */}
                    <td className="px-4 py-3.5 text-xs font-mono text-muted-foreground whitespace-nowrap">
                      {formatTime(log.dateAction)}
                    </td>

                    {/* Superviseur */}
                    <td className="px-4 py-3.5">
                      {log.nomSuperviseur ? (
                        <div className="flex items-center gap-2">
                          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10">
                            <User className="h-3.5 w-3.5 text-primary" />
                          </div>
                          <span className="font-semibold text-foreground text-sm whitespace-nowrap">
                            {log.prenomSuperviseur} {log.nomSuperviseur}
                          </span>
                        </div>
                      ) : (
                        <span className="text-muted-foreground text-xs">—</span>
                      )}
                    </td>

                    {/* Email */}
                    <td className="px-4 py-3.5 text-xs text-muted-foreground">
                      {log.emailSuperviseur ?? '—'}
                    </td>

                    {/* Action badge */}
                    <td className="px-4 py-3.5">
                      <ActionBadge action={log.action} />
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>Lignes par page :</span>
            <div className="neu-inset rounded-xl">
              <Select
                value={String(pageSize)}
                onValueChange={(v) => onPageSizeChange(Number(v))}
              >
                <SelectTrigger className="h-8 w-[70px] border-0 bg-transparent text-xs shadow-none focus:ring-0">
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
              Page {current + 1} sur {totalPages}
              {totalElements > 0 && (
                <span className="ml-1 text-muted-foreground/70">
                  ({totalElements.toLocaleString('fr-FR')} total)
                </span>
              )}
            </span>
          </div>

          <div className="neu-inset flex items-center gap-1 rounded-2xl p-1">
            <button
              onClick={() => onPageChange(Math.max(0, current - 1))}
              disabled={current === 0}
              className={cn(
                'flex h-7 w-7 items-center justify-center rounded-xl text-xs transition-all',
                current === 0
                  ? 'cursor-not-allowed text-muted-foreground/40'
                  : 'text-muted-foreground hover:bg-primary/10 hover:text-primary',
              )}
            >
              <ChevronLeft className="h-4 w-4" />
            </button>

            {buildPages(current + 1, totalPages).map((p, i) =>
              p === '…' ? (
                <span key={i} className="px-1 text-xs text-muted-foreground/60">…</span>
              ) : (
                <button
                  key={i}
                  onClick={() => onPageChange((p as number) - 1)}
                  className={cn(
                    'h-7 min-w-7 rounded-xl px-2 text-xs font-semibold transition-all',
                    current + 1 === p
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:bg-primary/10 hover:text-foreground',
                  )}
                >
                  {p}
                </button>
              ),
            )}

            <button
              onClick={() => onPageChange(Math.min(totalPages - 1, current + 1))}
              disabled={current >= totalPages - 1}
              className={cn(
                'flex h-7 w-7 items-center justify-center rounded-xl text-xs transition-all',
                current >= totalPages - 1
                  ? 'cursor-not-allowed text-muted-foreground/40'
                  : 'text-muted-foreground hover:bg-primary/10 hover:text-primary',
              )}
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
