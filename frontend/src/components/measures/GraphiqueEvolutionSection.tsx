import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Calendar as CalendarIcon } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { format, differenceInCalendarDays } from 'date-fns';
import { fr } from 'date-fns/locale';
import type { DateRange } from 'react-day-picker';
import { formatAxeGraphique } from '@/lib/utils';
import { getMesuresHistorique, getPointMesures, type MesureHistoriqueResponseDTO, type PointMesureResponse } from '@/api/measures';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';
import type { Metrique } from '@/api/alerting/seuils';

interface GraphiqueEvolutionSectionProps {
  modeFiltre?: 'global' | 'independant';
  filtreGlobal?: {
    idPointMesure?: number;
    metrique?: Metrique;
    dateDebut?: string;
    dateFin?: string;
  };
}

const RANGES = [
  { key: '1', label: '24h', days: 1, periode: '24h' },
  { key: '7', label: '7j', days: 7, periode: '7j' },
  { key: '30', label: '30j', days: 30, periode: '30j' },
  { key: '180', label: '6 mois', days: 180, periode: '6mois' },
  { key: '365', label: '1 an', days: 365, periode: '1an' },
];

const COLORS: Record<Metrique, string> = {
  TEMPERATURE: 'var(--chart-1)',
  HUMIDITE: 'var(--chart-2)',
};

const METRIC_LABELS: Record<Metrique, string> = {
  TEMPERATURE: 'Température',
  HUMIDITE: 'Humidité',
};

const METRIC_UNIT: Record<Metrique, string> = {
  TEMPERATURE: '°C',
  HUMIDITE: '%',
};

export function GraphiqueEvolutionSection({ modeFiltre = 'independant', filtreGlobal }: GraphiqueEvolutionSectionProps) {
  const [range, setRange] = useState('30');
  const [customRange, setCustomRange] = useState<DateRange | undefined>(() => {
    const now = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 30);
    return { from: start, to: now };
  });
  const [selectedPoint, setSelectedPoint] = useState<PointMesureResponse | null>(null);
  const [selectedMetrique, setSelectedMetrique] = useState<Metrique | null>(null);
  const [activeMetrics, setActiveMetrics] = useState<Metrique[]>(['TEMPERATURE']);
  const [historique, setHistorique] = useState<MesureHistoriqueResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pointDetails, setPointDetails] = useState<PointMesureResponse | null>(null);
  const [allPoints, setAllPoints] = useState<PointMesureResponse[]>([]);
  const [granularite, setGranularite] = useState<'HORAIRE' | 'JOURNALIERE'>('HORAIRE');
  
  // État pour suivre le bucket en cours (pour agrégation temps réel)
  const [currentBucket, setCurrentBucket] = useState<{
    bucketKey: string;
    sum: number;
    count: number;
  } | null>(null);

  const { connected, subscribeToMesures, unsubscribeFromMesures } = useDashboardWebSocket();

  const isGlobalMode = modeFiltre === 'global' && filtreGlobal?.idPointMesure;

  // Fonction utilitaire pour calculer la clé du bucket selon la granularité
  const getBucketKey = (timestamp: string, granulariteAppliquee: string): string => {
    const date = new Date(timestamp);
    switch (granulariteAppliquee) {
      case 'TRENTE_MIN':
        // Tronquer à l'heure, puis ajuster aux tranches de 30 min
        const hour = date.getHours();
        const thirtyMinBlock = Math.floor(date.getMinutes() / 30);
        date.setHours(hour, thirtyMinBlock * 30, 0, 0);
        return date.toISOString();
      case 'HORAIRE':
        date.setMinutes(0, 0, 0);
        return date.toISOString();
      case 'JOURNALIERE':
        date.setHours(0, 0, 0, 0);
        return date.toISOString();
      case 'MENSUELLE':
        date.setDate(1);
        date.setHours(0, 0, 0, 0);
        return date.toISOString();
      default:
        return timestamp;
    }
  };

  // Calcul des dates
  const dates = useMemo(() => {
    if (range === 'custom' && customRange?.from) {
      const end = customRange.to ?? customRange.from;
      return {
        dateDebut: customRange.from.toISOString(),
        dateFin: end.toISOString(),
        days: Math.max(1, differenceInCalendarDays(end, customRange.from) + 1),
      };
    }
    const days = RANGES.find((r) => r.key === range)?.days ?? 30;
    const now = new Date();
    const start = new Date(now);
    start.setDate(start.getDate() - days);
    return {
      dateDebut: start.toISOString(),
      dateFin: now.toISOString(),
      days,
    };
  }, [range, customRange]);

  // Fetch détails du point pour connaître les métriques applicables
  const fetchPointDetails = useCallback(async (pointId: number) => {
    try {
      const points = await getPointMesures();
      const point = points.find((p) => p.id === pointId);
      if (point) {
        setPointDetails(point);
        // Si la métrique actuelle n'est pas applicable, basculer sur TEMPERATURE
        if (selectedMetrique && !point.metriquesApplicables.includes(selectedMetrique)) {
          setSelectedMetrique('TEMPERATURE');
          setActiveMetrics(['TEMPERATURE']);
        }
      }
    } catch (e) {
      console.error('Erreur fetch point détails:', e);
    }
  }, [selectedMetrique]);

  // Fetch tous les points pour le sélecteur
  useEffect(() => {
    getPointMesures().then((points) => {
      setAllPoints(points);
      if (points.length > 0 && !selectedPoint) {
        const firstPoint = points[0];
        setSelectedPoint(firstPoint);
        setSelectedMetrique(firstPoint.metriquesApplicables[0] as Metrique);
        setActiveMetrics([firstPoint.metriquesApplicables[0] as Metrique]);
        setPointDetails(firstPoint);
      }
    });
  }, []);

  // Fetch historique
  const fetchHistorique = useCallback(async () => {
    if (!selectedPoint || !selectedMetrique) {
      setHistorique(null);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const rangeConfig = RANGES.find((r) => r.key === range);
      const params: any = {
        pointMesureId: selectedPoint.id,
        metrique: selectedMetrique,
        periode: range === 'custom' ? 'personnalise' : rangeConfig?.periode,
        dateDebut: dates.dateDebut,
        dateFin: dates.dateFin,
      };
      // Ajouter granularite selon la période
      if (range === '7') {
        // Sélecteur visible pour 7j : SEULEMENT HORAIRE et JOURNALIERE
        params.granularite = granularite;
      } else if (range === '1') {
        // 24h : TRENTE_MIN (fixe)
        params.granularite = 'TRENTE_MIN';
      } else if (range === '30') {
        // 30j : JOURNALIERE (fixe)
        params.granularite = 'JOURNALIERE';
      } else if (range === '180') {
        // 6mois : MENSUELLE (fixe)
        params.granularite = 'MENSUELLE';
      } else if (range === '365') {
        // 1an : MENSUELLE (fixe)
        params.granularite = 'MENSUELLE';
      } else if (range === 'custom') {
        // Personnalisé : selon la durée
        const duration = dates.dateFin && dates.dateDebut 
          ? new Date(dates.dateFin).getTime() - new Date(dates.dateDebut).getTime()
          : 0;
        const days = duration / (1000 * 60 * 60 * 24);
        if (days <= 1) {
          params.granularite = 'TRENTE_MIN';
        } else if (days <= 7) {
          params.granularite = 'HORAIRE';
        } else if (days <= 31) {
          params.granularite = 'JOURNALIERE';
        } else {
          params.granularite = 'MENSUELLE';
        }
      }
      const data = await getMesuresHistorique(params);
      setHistorique(data);
      // Initialiser le bucket en cours avec le dernier point de l'historique chargé
      if (data.points.length > 0) {
        const lastPoint = data.points[data.points.length - 1];
        const lastBucketKey = getBucketKey(lastPoint.horodatage, data.granulariteAppliquee);
        // Initialiser avec la moyenne comme somme et compteur=1 (approximation acceptable pour temps réel)
        setCurrentBucket({ bucketKey: lastBucketKey, sum: lastPoint.valeur, count: 1 });
      } else {
        setCurrentBucket(null);
      }
    } catch (e) {
      console.error('Erreur fetch historique:', e);
      setError('Impossible de charger l\'historique');
    } finally {
      setLoading(false);
    }
  }, [selectedPoint, selectedMetrique, dates, range, granularite]);

  // Fetch initial et changements
  useEffect(() => {
    if (selectedPoint) {
      fetchPointDetails(selectedPoint.id);
    }
  }, [selectedPoint, fetchPointDetails]);

  useEffect(() => {
    fetchHistorique();
  }, [fetchHistorique]);

  // Abonnement WebSocket
  useEffect(() => {
    if (!selectedPoint || !selectedMetrique) return;

    const topic = `/topic/mesures/${selectedPoint.id}/${selectedMetrique}`;
    const unsubscribe = subscribeToMesures(selectedPoint.id, selectedMetrique, (data: unknown) => {
      const message = data as { valeur: number; timestamp: string };
      setHistorique((prev) => {
        if (!prev) return prev;
        
        const granulariteAppliquee = prev.granulariteAppliquee;
        const newBucketKey = getBucketKey(message.timestamp, granulariteAppliquee);
        
        // Trouver le dernier point existant
        const lastPoint = prev.points[prev.points.length - 1];
        const lastBucketKey = lastPoint ? getBucketKey(lastPoint.horodatage, granulariteAppliquee) : null;
        
        // Si le nouveau point appartient au même bucket que le dernier
        if (lastPoint && lastBucketKey === newBucketKey) {
          // Recalculer la moyenne du bucket en cours
          const newSum = (currentBucket?.sum ?? 0) + message.valeur;
          const newCount = (currentBucket?.count ?? 0) + 1;
          const newAverage = newSum / newCount;
          
          setCurrentBucket({ bucketKey: newBucketKey, sum: newSum, count: newCount });
          
          // Mettre à jour le dernier point avec la nouvelle moyenne
          const updatedPoints = [...prev.points];
          updatedPoints[updatedPoints.length - 1] = {
            horodatage: newBucketKey,
            valeur: newAverage,
          };
          
          return {
            ...prev,
            points: updatedPoints,
          };
        } else {
          // Nouveau bucket : créer un nouveau point
          setCurrentBucket({ bucketKey: newBucketKey, sum: message.valeur, count: 1 });
          
          const newPoint = {
            horodatage: newBucketKey,
            valeur: message.valeur,
          };
          
          // Retirer les points hors de la fenêtre de période
          const cutoffDate = new Date(dates.dateDebut);
          const filteredPoints = [...prev.points, newPoint].filter(
            (p) => new Date(p.horodatage) >= cutoffDate
          );
          
          return {
            ...prev,
            points: filteredPoints,
          };
        }
      });
    });

    return () => {
      unsubscribeFromMesures(selectedPoint.id, selectedMetrique);
      unsubscribe();
    };
  }, [selectedPoint, selectedMetrique, dates, subscribeToMesures, unsubscribeFromMesures, currentBucket, getBucketKey]);

  // Toggle métrique
  const toggleMetric = (m: Metrique) => {
    if (!pointDetails || !pointDetails.metriquesApplicables.includes(m)) return;
    setActiveMetrics((cur) =>
      cur.includes(m) ? (cur.length > 1 ? cur.filter((x) => x !== m) : cur) : [...cur, m]
    );
  };

  const active = activeMetrics.length === 0 ? [selectedMetrique || 'TEMPERATURE'] : activeMetrics;

  // Format de l'axe des abscisses selon la période
  const getDateFormat = useCallback(() => {
    if (range === 'custom' && customRange?.from && customRange?.to) {
      const days = differenceInCalendarDays(customRange.to, customRange.from);
      if (days <= 2) return 'dd/MM HH:mm'; // Moins de 2 jours : heure
      if (days <= 31) return 'dd/MM'; // Moins d'un mois : jour
      return 'MMM yyyy'; // Plus d'un mois : mois
    }
    const days = RANGES.find((r) => r.key === range)?.days ?? 30;
    if (days <= 2) return 'dd/MM HH:mm'; // 24h : heure
    if (days <= 31) return 'dd/MM'; // 7j, 30j : jour
    return 'MMM yyyy'; // 3 mois, 6 mois, 1 an : mois
  }, [range, customRange]);

  // Données formatées pour le graphe
  const chartData = useMemo(() => {
    if (!historique) return [];
    return historique.points.map((p) => ({
      date: formatAxeGraphique(p.horodatage, historique.granulariteAppliquee),
      TEMPERATURE: selectedMetrique === 'TEMPERATURE' ? Number(p.valeur) : undefined,
      HUMIDITE: selectedMetrique === 'HUMIDITE' ? Number(p.valeur) : undefined,
      valeur: Number(p.valeur),
    }));
  }, [historique, selectedMetrique]);

  // Calcul du domaine Y pour inclure les seuils absolus s'ils existent
  const yDomain = useMemo(() => {
    if (!historique || !historique.seuilAbsolu || historique.points.length === 0) {
      return ['auto', 'auto'];
    }

    const values = historique.points.map((p) => Number(p.valeur));
    const dataMin = Math.min(...values);
    const dataMax = Math.max(...values);

    // Coercion défensive : BigDecimal Java peut arriver comme string JSON
    const seuilMin = Number(historique.seuilAbsolu.valeurMin);
    const seuilMax = Number(historique.seuilAbsolu.valeurMax);

    // Vérifier que les valeurs sont bien des nombres valides
    if (isNaN(seuilMin) || isNaN(seuilMax) || isNaN(dataMin) || isNaN(dataMax)) {
      console.warn('yDomain: valeurs NaN détectées', { seuilMin, seuilMax, dataMin, dataMax });
      return ['auto', 'auto'];
    }

    // Déterminer les limites min et max incluant les seuils
    const finalMin = Math.min(dataMin, seuilMin);
    const finalMax = Math.max(dataMax, seuilMax);

    // Marge de 5% de l'écart total (minimum 1 pour éviter division par zéro)
    const diff = Math.max(1, finalMax - finalMin);
    const margin = diff * 0.05;

    // Arrondir à 1 décimale pour éviter les nombres irrationnels
    const roundedMin = Math.round((finalMin - margin) * 10) / 10;
    const roundedMax = Math.round((finalMax + margin) * 10) / 10;

    return [roundedMin, roundedMax];
  }, [historique]);

  // Tooltip personnalisé
  const ChartTip = ({ active, payload, label }: any) => {
    if (!active || !payload || !payload.length) return null;
    return (
      <div className="neu-card-sm rounded-2xl border border-border/60 bg-[color:var(--surface-raised)] px-3 py-2 text-xs">
        <p className="font-semibold">{label}</p>
        {payload.map((p: any) => (
          <div key={p.dataKey} className="mt-1 flex items-center gap-2">
            <span className="h-2 w-2 rounded-full" style={{ background: p.color }} />
            <span className="text-muted-foreground">{p.name}:</span>
            <span className="font-semibold">
              {p.value} {METRIC_UNIT[p.dataKey as Metrique]}
            </span>
          </div>
        ))}
      </div>
    );
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
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold">Évolution des métriques</h2>
            {connected && selectedPoint && selectedMetrique && (
              <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" title="Connecté en temps réel" />
            )}
          </div>
          <p className="text-sm text-muted-foreground">courbes lissées</p>
        </div>

        {/* Sélecteurs si mode indépendant */}
        {modeFiltre === 'independant' && (
          <div className="flex flex-col gap-2">
            {/* Ligne période — tous les boutons dans le même neu-inset */}
            <div className="neu-inset flex gap-1 rounded-2xl p-1 w-fit">
              {RANGES.map((r) => (
                <button
                  key={r.key}
                  onClick={() => {
                    setRange(r.key);
                    setCustomRange(undefined);
                  }}
                  className={
                    'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                    (range === r.key
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground')
                  }
                >
                  {r.label}
                </button>
              ))}
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    onClick={() => {
                      setRange('custom');
                      setCustomRange(undefined);
                    }}
                    className={
                      'flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                      (range === 'custom'
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:text-foreground')
                    }
                  >
                    <CalendarIcon className="h-3.5 w-3.5" />
                    {range === 'custom' && customRange?.from
                      ? customRange.to
                        ? `${format(customRange.from, 'd MMM', { locale: fr })} - ${format(customRange.to, 'd MMM', { locale: fr })}`
                        : format(customRange.from, 'd MMM yyyy', { locale: fr })
                      : 'Personnalisé'}
                  </button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="end">
                  <Calendar
                    mode="range"
                    selected={customRange}
                    defaultMonth={new Date()}
                    onSelect={(r) => {
                      setCustomRange(r);
                      if (r?.from) setRange('custom');
                    }}
                    locale={fr}
                    numberOfMonths={1}
                    className="p-3 pointer-events-auto"
                    initialFocus
                    modifiers={{
                      today: undefined
                    }}
                  />
                </PopoverContent>
              </Popover>
            </div>

            {/* Granularité (visible uniquement pour 7j) */}
            {range === '7' && (
              <div className="neu-inset flex gap-1 rounded-2xl p-1 w-fit">
                <button
                  onClick={() => setGranularite('HORAIRE')}
                  className={
                    'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                    (granularite === 'HORAIRE'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground')
                  }
                >
                  Horaire
                </button>
                <button
                  onClick={() => setGranularite('JOURNALIERE')}
                  className={
                    'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                    (granularite === 'JOURNALIERE'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground')
                  }
                >
                  Journalière
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Sélecteur point + métrique si mode indépendant */}
      {modeFiltre === 'independant' && (
        <div className="mt-4 flex flex-wrap items-center gap-4">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
              Point de mesure
            </label>
            <Select
              value={selectedPoint?.id.toString() ?? ''}
              onValueChange={(value) => {
                getPointMesures().then((points) => {
                  const point = points.find((p) => p.id.toString() === value);
                  if (point) {
                    setSelectedPoint(point);
                    setSelectedMetrique(point.metriquesApplicables[0] as Metrique);
                    setActiveMetrics([point.metriquesApplicables[0] as Metrique]);
                  }
                });
              }}
            >
              <SelectTrigger className="w-full bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)]">
                <SelectValue placeholder="Sélectionner un point" />
              </SelectTrigger>
              <SelectContent>
                {allPoints.length > 0 ? (
                  allPoints.map((point) => (
                    <SelectItem key={point.id} value={point.id.toString()}>
                      {point.nom}
                    </SelectItem>
                  ))
                ) : (
                  <SelectItem value="">Chargement...</SelectItem>
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="flex-1 min-w-[150px]">
            <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
              Métrique
            </label>
            <Select
              value={selectedMetrique ?? ''}
              onValueChange={(value) => {
                setSelectedMetrique(value as Metrique);
                setActiveMetrics([value as Metrique]);
              }}
              disabled={!selectedPoint}
            >
              <SelectTrigger className="w-full bg-[color:var(--surface)] shadow-[var(--shadow-neu-inset)]">
                <SelectValue placeholder="Sélectionner" />
              </SelectTrigger>
              <SelectContent>
                {pointDetails?.metriquesApplicables.map((m: string) => (
                  <SelectItem key={m} value={m}>
                    {m === 'TEMPERATURE' ? 'Température' : 'Humidité'}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      )}



      {/* Graphe */}
      <div className="relative mt-6 h-[340px] w-full">
        {loading && (
          <div className="absolute inset-0 z-20 flex items-center justify-center rounded-2xl bg-background/60 backdrop-blur-[2px] transition-all">
            <div className="flex flex-col items-center gap-2">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
              <span className="text-xs font-semibold text-muted-foreground">Chargement des données...</span>
            </div>
          </div>
        )}
        {!historique || chartData.length === 0 ? (
          <div className="flex h-full items-center justify-center text-muted-foreground text-sm">
            {selectedPoint && selectedMetrique
              ? 'Aucune donnée pour cette période'
              : 'Sélectionnez un point de mesure et une métrique'}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={340}>
            {active.length === 1 ? (
              <AreaChart data={chartData} margin={{ top: 10, right: 15, left: 10, bottom: 5 }}>
                <defs>
                  <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={COLORS[active[0]]} stopOpacity={0.55} />
                    <stop offset="100%" stopColor={COLORS[active[0]]} stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis
                  dataKey="date"
                  stroke="var(--muted-foreground)"
                  fontSize={11}
                  tickLine={{ stroke: 'var(--border)' }}
                  axisLine={{ stroke: 'var(--border)', strokeWidth: 1.5 }}
                />
                <YAxis
                  domain={yDomain}
                  stroke="var(--muted-foreground)"
                  fontSize={11}
                  tickLine={{ stroke: 'var(--border)' }}
                  axisLine={{ stroke: 'var(--border)', strokeWidth: 1.5 }}
                  tickFormatter={(value) => value.toFixed(1)}
                />
                <Tooltip content={<ChartTip />} />
                <Area
                  type="monotone"
                  dataKey="valeur"
                  stroke={COLORS[active[0]]}
                  strokeWidth={2.5}
                  fill="url(#grad)"
                  name={METRIC_LABELS[active[0]]}
                />
                {historique.seuilAbsolu && (
                  <ReferenceLine
                    y={Number(historique.seuilAbsolu.valeurMax)}
                    stroke="#ef4444"
                    strokeDasharray="6 6"
                    strokeWidth={2}
                    label={{ value: `Max ${historique.seuilAbsolu.valeurMax}`, fill: '#ef4444', fontSize: 11, fontWeight: 'bold', position: 'insideTopRight' }}
                  />
                )}
                {historique.seuilAbsolu && (
                  <ReferenceLine
                    y={Number(historique.seuilAbsolu.valeurMin)}
                    stroke="#f97316"
                    strokeDasharray="6 6"
                    strokeWidth={2}
                    label={{ value: `Min ${historique.seuilAbsolu.valeurMin}`, fill: '#f97316', fontSize: 11, fontWeight: 'bold', position: 'insideBottomRight' }}
                  />
                )}
              </AreaChart>
            ) : (
              <LineChart data={chartData} margin={{ top: 10, right: 15, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis
                  dataKey="date"
                  stroke="var(--muted-foreground)"
                  fontSize={11}
                  tickLine={{ stroke: 'var(--border)' }}
                  axisLine={{ stroke: 'var(--border)', strokeWidth: 1.5 }}
                />
                <YAxis
                  domain={yDomain}
                  stroke="var(--muted-foreground)"
                  fontSize={11}
                  tickLine={{ stroke: 'var(--border)' }}
                  axisLine={{ stroke: 'var(--border)', strokeWidth: 1.5 }}
                  tickFormatter={(value) => value.toFixed(1)}
                />
                <Tooltip content={<ChartTip />} />
                {active.map((m) => (
                  <Line
                    key={m}
                    type="monotone"
                    dataKey={m}
                    stroke={COLORS[m]}
                    strokeWidth={2.5}
                    name={METRIC_LABELS[m]}
                  />
                ))}
                {historique.seuilAbsolu && (
                  <ReferenceLine
                    y={Number(historique.seuilAbsolu.valeurMax)}
                    stroke="#ef4444"
                    strokeDasharray="6 6"
                    strokeWidth={2}
                    label={{ value: `Max ${historique.seuilAbsolu.valeurMax}`, fill: '#ef4444', fontSize: 11, fontWeight: 'bold', position: 'insideTopRight' }}
                  />
                )}
                {historique.seuilAbsolu && (
                  <ReferenceLine
                    y={Number(historique.seuilAbsolu.valeurMin)}
                    stroke="#f97316"
                    strokeDasharray="6 6"
                    strokeWidth={2}
                    label={{ value: `Min ${historique.seuilAbsolu.valeurMin}`, fill: '#f97316', fontSize: 11, fontWeight: 'bold', position: 'insideBottomRight' }}
                  />
                )}
              </LineChart>
            )}
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
