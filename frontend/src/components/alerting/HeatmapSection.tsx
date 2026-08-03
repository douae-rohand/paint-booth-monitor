import { useState, useEffect, useCallback } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import {
  getHeatmapMois,
  getHeatmapJour,
  type HeatmapJourDTO,
  type HeatmapParams,
  type DetailJourAlertesDTO,
} from '@/api/alertes';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { getPointMesures, type PointMesureResponse } from '@/api/measures';
import type { Metrique } from '@/api/alerting/seuils';

interface HeatmapSectionProps {
  modeFiltre?: 'global' | 'independant';
  filtreGlobal?: {
    idPointMesure?: number;
    metrique?: Metrique;
  };
}

export function HeatmapSection({ modeFiltre = 'independant', filtreGlobal }: HeatmapSectionProps) {
  const [currentMonthDate, setCurrentMonthDate] = useState(() => new Date());
  const [heatmapData, setHeatmapData] = useState<HeatmapJourDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [metricFilter, setMetricFilter] = useState<'all' | 'TEMPERATURE' | 'HUMIDITE'>('all');
  const [selectedPoint, setSelectedPoint] = useState<PointMesureResponse | null>(null);
  const [points, setPoints] = useState<PointMesureResponse[]>([]);
  const [selectedDayDetail, setSelectedDayDetail] = useState<DetailJourAlertesDTO | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  const { connected, subscribeToAlertes } = useDashboardWebSocket();

  const isGlobalMode = modeFiltre === 'global' && filtreGlobal?.idPointMesure;

  // Metrics available based on selected point type
  const availableMetrics: Array<{ value: 'all' | 'TEMPERATURE' | 'HUMIDITE'; label: string }> = [
    { value: 'all', label: 'Toutes métriques' },
    { value: 'TEMPERATURE', label: 'Température' },
    ...(selectedPoint?.typeEmplacement !== 'ETUVE' ? [{ value: 'HUMIDITE' as 'HUMIDITE', label: 'Humidité' }] : []),
  ];

  // Fetch points pour le sélecteur
  useEffect(() => {
    getPointMesures().then(setPoints).catch(console.error);
  }, []);

  // Fetch heatmap mois
  const fetchHeatmap = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params: HeatmapParams = {
        annee: currentMonthDate.getFullYear(),
        mois: currentMonthDate.getMonth() + 1,
        pointMesureId: isGlobalMode ? filtreGlobal?.idPointMesure : selectedPoint?.id,
        metrique: metricFilter === 'all' ? undefined : metricFilter,
      };

      const data = await getHeatmapMois(params);
      setHeatmapData(data);
    } catch (e) {
      console.error('Erreur fetch heatmap:', e);
      setError('Impossible de charger la heatmap');
    } finally {
      setLoading(false);
    }
  }, [currentMonthDate, metricFilter, selectedPoint, isGlobalMode, filtreGlobal]);

  useEffect(() => {
    fetchHeatmap();
  }, [fetchHeatmap]);

  // Fetch détail jour
  const fetchDayDetail = useCallback(async (date: string) => {
    try {
      setLoadingDetail(true);
      const params = {
        date,
        pointMesureId: isGlobalMode ? filtreGlobal?.idPointMesure : selectedPoint?.id,
        metrique: metricFilter === 'all' ? undefined : metricFilter,
      };
      const data = await getHeatmapJour(params);
      setSelectedDayDetail(data);
    } catch (e) {
      console.error('Erreur fetch détail jour:', e);
    } finally {
      setLoadingDetail(false);
    }
  }, [isGlobalMode, filtreGlobal, selectedPoint, metricFilter]);

  // Abonnement WebSocket
  useEffect(() => {
    const now = new Date();
    const isCurrentMonth =
      currentMonthDate.getFullYear() === now.getFullYear() &&
      currentMonthDate.getMonth() === now.getMonth();

    if (!isCurrentMonth) return;

    const unsubscribe = subscribeToAlertes((data: unknown) => {
      const alerte = data as { createdAt: string };
      const alertDate = new Date(alerte.createdAt);
      const isToday =
        alertDate.getDate() === now.getDate() &&
        alertDate.getMonth() === now.getMonth() &&
        alertDate.getFullYear() === now.getFullYear();

      if (isToday) {
        setHeatmapData((prev) => {
          const index = prev.findIndex((d) => d.date === alertDate.toISOString().split('T')[0]);
          if (index >= 0) {
            const updated = [...prev];
            updated[index] = {
              ...updated[index],
              nombreDepassements: updated[index].nombreDepassements + 1,
            };
            return updated;
          }
          return prev;
        });

        // Si popup ouverte, recharger
        if (selectedDayDetail && selectedDayDetail.date === alertDate.toISOString().split('T')[0]) {
          fetchDayDetail(selectedDayDetail.date);
        }
      }
    });
    return unsubscribe;
  }, [currentMonthDate, selectedDayDetail, fetchDayDetail, subscribeToAlertes]);

  const handlePrevMonth = () => {
    setCurrentMonthDate((d) => new Date(d.getFullYear(), d.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonthDate((d) => new Date(d.getFullYear(), d.getMonth() + 1, 1));
  };

  const handleDayClick = (day: HeatmapJourDTO) => {
    fetchDayDetail(day.date);
  };

  const closeDetail = () => {
    setSelectedDayDetail(null);
  };

  if (error) {
    return (
      <div className="neu-card p-6">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  return (
    <div className="neu-card p-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <h3 className="text-base font-bold">Heatmap des alertes</h3>
          {connected && <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />}
        </div>
        <p className="text-xs text-muted-foreground">Consulter et filtrer les alertes mensuelles</p>
      </div>

      {/* Controls */}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        {/* Navigation Mois */}
        <div className="neu-inset flex items-center gap-1.5 p-1 rounded-xl">
          <button
            type="button"
            onClick={handlePrevMonth}
            className="p-1 hover:bg-primary/20 hover:text-primary rounded-lg transition-colors"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-xs font-bold capitalize px-1 min-w-[100px] text-center text-foreground">
            {currentMonthDate.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })}
          </span>
          <button
            type="button"
            onClick={handleNextMonth}
            className="p-1 hover:bg-primary/20 hover:text-primary rounded-lg transition-colors"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>

        {/* Filtres si mode indépendant */}
        {modeFiltre === 'independant' && (
          <div className="neu-inset flex items-center gap-1 p-1 rounded-2xl">
            {/* Point de mesure */}
            <Select
              value={selectedPoint?.id.toString() ?? ''}
              onValueChange={(v) => {
                const point = v ? points.find((p) => p.id.toString() === v) ?? null : null;
                setSelectedPoint(point);
                // Reset metric filter if point doesn't support HUMIDITE
                if (point?.typeEmplacement === 'ETUVE' && metricFilter === 'HUMIDITE') {
                  setMetricFilter('all');
                }
              }}
            >
              <SelectTrigger className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 h-auto text-xs font-semibold border-0 shadow-none bg-transparent hover:text-foreground text-muted-foreground focus:ring-0 focus:ring-offset-0 pointer-events-auto cursor-pointer">
                <span className="max-w-[120px] truncate">
                  {selectedPoint ? selectedPoint.nom : 'Tous points'}
                </span>
              </SelectTrigger>
              <SelectContent className="text-xs">
                <SelectItem value="">Tous points</SelectItem>
                {points.map((p) => (
                  <SelectItem key={p.id} value={p.id.toString()}>
                    {p.nom}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <span className="self-center text-muted-foreground/30 text-xs select-none">|</span>

            {/* Métrique */}
            <Select
              value={metricFilter}
              onValueChange={(v) => setMetricFilter(v as 'all' | 'TEMPERATURE' | 'HUMIDITE')}
            >
              <SelectTrigger className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 h-auto text-xs font-semibold border-0 shadow-none bg-transparent hover:text-foreground text-muted-foreground focus:ring-0 focus:ring-offset-0 pointer-events-auto cursor-pointer">
                <span className="max-w-[110px] truncate">
                  {metricFilter === 'all'
                    ? 'Toutes métriques'
                    : metricFilter === 'TEMPERATURE'
                      ? 'Température'
                      : 'Humidité'}
                </span>
              </SelectTrigger>
              <SelectContent className="text-xs">
                {availableMetrics.map((m) => (
                  <SelectItem key={m.value} value={m.value}>{m.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </div>

      {/* Grille Heatmap */}
      {loading ? (
        <div className="mt-4 grid grid-cols-7 gap-2">
          {[...Array(35)].map((_, i) => (
            <div key={i} className="aspect-square rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="mt-4 grid grid-cols-7 gap-2 text-center text-[10px] text-muted-foreground">
          {['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((d, i) => (
            <div key={i} className="font-semibold py-1">
              {d}
            </div>
          ))}
          {(() => {
            if (!heatmapData.length) return null;
            const first = new Date(heatmapData[0].date).getDay();
            const pad = (first + 6) % 7;
            return Array.from({ length: pad }).map((_, i) => (
              <div key={`pad-${i}`} />
            ));
          })()}
          {heatmapData.map((d) => {
            const intensity = Math.min(d.nombreDepassements / 8, 1);
            const bg =
              d.nombreDepassements === 0
                ? 'var(--surface)'
                : `color-mix(in oklab, var(--primary) ${35 + intensity * 65}%, white)`;

            return (
              <button
                key={d.date}
                onClick={() => handleDayClick(d)}
                className="aspect-square rounded-lg text-[10px] font-semibold flex items-center justify-center cursor-help transition-all duration-150 hover:scale-105"
                style={{
                  background: bg,
                  color:
                    d.nombreDepassements > 0 && intensity > 0.5
                      ? 'var(--primary-foreground)'
                      : 'var(--muted-foreground)',
                  boxShadow:
                    d.nombreDepassements === 0
                      ? 'var(--shadow-neu-inset)'
                      : 'var(--shadow-neu-sm)',
                }}
              >
                {new Date(d.date).getDate()}
              </button>
            );
          })}
        </div>
      )}

      {/* Légende */}
      <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
        <span>Moins</span>
        <div className="flex gap-1">
          {[0.3, 0.5, 0.7, 0.85, 0.98].map((v) => (
            <div
              key={v}
              className="h-3 w-6 rounded"
              style={{
                background: `color-mix(in oklab, var(--primary) ${v * 100}%, white)`,
              }}
            />
          ))}
        </div>
        <span>Plus</span>
      </div>

      {/* Modal détail jour */}
      {selectedDayDetail && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={closeDetail}>
          <div
            className="bg-background border border-border/50 shadow-xl rounded-2xl p-6 max-w-md w-full max-h-[80vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h4 className="font-bold text-foreground">
                {format(new Date(selectedDayDetail.date), 'EEEE d MMMM yyyy', { locale: fr })}
              </h4>
              <button
                onClick={closeDetail}
                className="text-muted-foreground hover:text-foreground transition-colors h-7 w-7 rounded-full flex items-center justify-center hover:bg-muted"
              >
                ✕
              </button>
            </div>

            {loadingDetail ? (
              <div className="animate-pulse space-y-2">
                <div className="h-4 bg-muted rounded" />
                <div className="h-4 bg-muted rounded" />
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-sm font-semibold text-foreground">
                  {selectedDayDetail.nombreTotalDepassements}{' '}
                  {selectedDayDetail.nombreTotalDepassements <= 1
                    ? 'alerte'
                    : 'alertes'}
                </p>
                <div className="space-y-2">
                  {selectedDayDetail.details.map((det, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-xl border border-border/60 bg-muted/30 text-xs space-y-1"
                    >
                      <div className="flex justify-between font-medium text-foreground">
                        <span>
                          {det.nomPointMesure} -{' '}
                          {det.metrique === 'TEMPERATURE' ? 'Température' : 'Humidité'}
                        </span>
                        <span className="text-[color:var(--danger)] font-bold">{det.nombreDepassements} {det.nombreDepassements <= 1 ? 'alerte' : 'alertes'}</span>
                      </div>
                      <div className="flex justify-between text-muted-foreground">
                        <span>Valeur max: {det.valeurMaxAtteinte}</span>
                        {det.seuilConfigure && (
                          <span>
                            Seuil: {det.seuilConfigure.valeurMin} - {det.seuilConfigure.valeurMax}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
