import { useState, useEffect, useCallback } from 'react';
import {
  listerRapports,
  telechargerRapport,
  type RapportPDFResponse,
  type StatutGeneration,
  type TypeRapport,
} from '@/api/reports';
import {
  Download,
  FileText,
  Loader2,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Clock,
  CheckCircle2,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';

interface HistoriqueRapportsListeProps {
  refreshTrigger?: number;
}

export function HistoriqueRapportsListe({ refreshTrigger = 0 }: HistoriqueRapportsListeProps) {
  const [rapports, setRapports] = useState<RapportPDFResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const fetchRapports = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await listerRapports(page, pageSize);
      setRapports(data.content ?? []);
      setTotalPages(data.page?.totalPages ?? 0);
      setTotalElements(data.page?.totalElements ?? 0);
    } catch (e: any) {
      console.error('Erreur chargement des rapports :', e);
      setError('Impossible de charger la liste des rapports.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchRapports();
  }, [fetchRapports, refreshTrigger]);

  const handleDownload = async (rapport: RapportPDFResponse) => {
    if (rapport.statutGeneration !== 'TERMINE') return;
    try {
      setDownloadingId(rapport.idRapport);
      await telechargerRapport(rapport.idRapport, rapport.nomFichier || `rapport-${rapport.idRapport}.pdf`);
    } catch (err: any) {
      console.error('Erreur de téléchargement :', err);
    } finally {
      setDownloadingId(null);
    }
  };

  const formatFileSize = (bytes: number | null): string => {
    if (bytes === null || bytes === undefined || bytes <= 0) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} Mo`;
  };

  const formatDateStr = (dateStr: string | null): string => {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return '—';
      return format(d, 'd MMM yyyy HH:mm', { locale: fr });
    } catch {
      return '—';
    }
  };

  const getTypeBadge = (type: TypeRapport) => {
    switch (type) {
      case 'JOURNALIER':
        return <Badge className="bg-blue-50 text-blue-700 border-blue-200 shadow-none font-semibold">Journalier</Badge>;
      case 'HEBDOMADAIRE':
        return <Badge className="bg-purple-50 text-purple-700 border-purple-200 shadow-none font-semibold">Hebdomadaire</Badge>;
      case 'MENSUEL':
        return <Badge className="bg-indigo-50 text-indigo-700 border-indigo-200 shadow-none font-semibold">Mensuel</Badge>;
      case 'PERSONNALISE':
      default:
        return <Badge className="bg-slate-50 text-slate-700 border-slate-200 shadow-none font-semibold">Personnalisé</Badge>;
    }
  };

  const getStatutBadge = (statut: StatutGeneration) => {
    switch (statut) {
      case 'TERMINE':
        return (
          <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200 shadow-none font-semibold flex items-center gap-1">
            <CheckCircle2 className="h-3 w-3 text-emerald-600" />
            <span>Terminé</span>
          </Badge>
        );
      case 'EN_COURS':
        return (
          <Badge className="bg-amber-50 text-amber-700 border-amber-200 shadow-none font-semibold flex items-center gap-1">
            <Clock className="h-3 w-3 text-amber-600 animate-pulse" />
            <span>En cours</span>
          </Badge>
        );
      case 'ECHEC':
      default:
        return (
          <Badge className="bg-rose-50 text-rose-700 border-rose-200 shadow-none font-semibold flex items-center gap-1">
            <AlertCircle className="h-3 w-3 text-rose-600" />
            <span>Échec</span>
          </Badge>
        );
    }
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setPage(0);
  };

  const current = Math.min(page, Math.max(0, totalPages - 1));

  return (
    <div className="neu-card p-6 flex flex-col gap-5">
      {/* Header avec titre & rafraîchissement */}
      <div className="flex items-center justify-between border-b border-border/40 pb-4">
        <div>
          <h3 className="text-base font-bold">Historique de vos rapports</h3>
          <p className="text-xs text-muted-foreground">
            {totalElements > 0
              ? `${totalElements} rapport(s) généré(s) au total`
              : 'Consultez et téléchargez vos rapports PDF'}
          </p>
        </div>
        <button
          onClick={fetchRapports}
          disabled={loading}
          title="Actualiser la liste"
          className="neu-pressable flex h-9 w-9 items-center justify-center rounded-xl text-muted-foreground hover:text-foreground transition-all"
        >
          <RefreshCw className={cn('h-4 w-4', loading && 'animate-spin text-primary')} />
        </button>
      </div>

      {/* Message Erreur */}
      {error && (
        <div className="flex items-center gap-2 text-xs text-destructive bg-destructive/10 border border-destructive/20 p-3 rounded-2xl">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Table des rapports */}
      <div className="overflow-hidden rounded-2xl border border-border/60 bg-[color:var(--surface)]">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-[color:var(--surface-raised)] border-b border-border/60">
              <tr className="text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
                <th className="px-4 py-3">Point de mesure</th>
                <th className="px-4 py-3">Période</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Généré le</th>
                <th className="px-4 py-3">Statut</th>
                <th className="px-4 py-3">Taille</th>
                <th className="px-4 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40">
              {loading && rapports.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-sm text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <Loader2 className="h-6 w-6 animate-spin text-primary" />
                      <span>Chargement de l'historique...</span>
                    </div>
                  </td>
                </tr>
              ) : rapports.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-sm text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <FileText className="h-8 w-8 text-muted-foreground/30" />
                      <span className="font-semibold text-foreground">Aucun rapport généré pour le moment</span>
                      <span className="text-xs">Utilisez le formulaire ci-dessus pour générer votre premier rapport.</span>
                    </div>
                  </td>
                </tr>
              ) : (
                rapports.map((rapport, i) => {
                  const isDownloading = downloadingId === rapport.idRapport;
                  const isReady = rapport.statutGeneration === 'TERMINE';

                  return (
                    <tr
                      key={rapport.idRapport}
                      className={cn(
                        'transition-colors hover:bg-primary/5',
                        i % 2 === 1 ? 'bg-[color:var(--surface-muted)]/30' : 'bg-transparent',
                      )}
                    >
                      {/* Point de mesure */}
                      <td className="px-4 py-3.5 font-semibold text-foreground">
                        {rapport.pointMesure?.nom || `Point #${rapport.pointMesure?.id}`}
                      </td>

                      {/* Période */}
                      <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                        {formatDateStr(rapport.periodeDebut)} → {formatDateStr(rapport.periodeFin)}
                      </td>

                      {/* Type */}
                      <td className="px-4 py-3.5">
                        {getTypeBadge(rapport.typeRapport)}
                      </td>

                      {/* Date de génération */}
                      <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                        {formatDateStr(rapport.dateRapport)}
                      </td>

                      {/* Statut */}
                      <td className="px-4 py-3.5">
                        {getStatutBadge(rapport.statutGeneration)}
                      </td>

                      {/* Taille */}
                      <td className="px-4 py-3.5 text-xs text-muted-foreground font-mono">
                        {formatFileSize(rapport.tailleFichier)}
                      </td>

                      {/* Action Télécharger */}
                      <td className="px-4 py-3.5 text-right">
                        <button
                          type="button"
                          onClick={() => handleDownload(rapport)}
                          disabled={!isReady || isDownloading}
                          className={cn(
                            'inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-all',
                            isReady && !isDownloading
                              ? 'bg-primary text-primary-foreground hover:opacity-90 shadow-sm'
                              : 'bg-muted text-muted-foreground opacity-50 cursor-not-allowed',
                          )}
                        >
                          {isDownloading ? (
                            <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          ) : (
                            <Download className="h-3.5 w-3.5" />
                          )}
                          <span>{isDownloading ? 'Téléchargement...' : 'Télécharger'}</span>
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>Rapports par page :</span>
            <div className="neu-inset rounded-xl">
              <Select value={String(pageSize)} onValueChange={(v) => handlePageSizeChange(Number(v))}>
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
            </span>
          </div>

          <div className="neu-inset flex items-center gap-1 rounded-2xl p-1">
            <button
              onClick={() => handlePageChange(Math.max(0, current - 1))}
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
                <span key={i} className="px-1 text-xs text-muted-foreground/60">
                  …
                </span>
              ) : (
                <button
                  key={i}
                  onClick={() => handlePageChange((p as number) - 1)}
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
              onClick={() => handlePageChange(Math.min(totalPages - 1, current + 1))}
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
