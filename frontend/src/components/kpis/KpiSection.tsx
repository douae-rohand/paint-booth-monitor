import { useState, useEffect, useCallback } from 'react';
import { AlertTriangle, CheckCircle, Clock, Activity, Calendar as CalendarIcon } from 'lucide-react';
import { getKpis, type KpiResponseDTO, type KpiParams } from '@/api/kpis';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';
import { PointMesureMetriqueSelector } from '@/components/alerting/PointMesureMetriqueSelector';
import { formatDureeHeures } from '@/lib/utils';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import type { DateRange } from 'react-day-picker';
import type { PointMesure, Metrique } from '@/api/alerting/seuils';
import { usePointMesures } from '@/hooks/useSeuils';

// State pour suivre les valeurs animées
type AnimatedField = 'alertesActives' | 'nbPointsEnAnomalie' | 'tauxConformite' | 'tempsMoyenEntreIncidentsHeures' | 'tempsMoyenRetourNormalHeures';

interface KpiSectionProps {
  modeFiltre?: 'global' | 'independant';
  filtreGlobal?: {
    idPointMesure?: number;
    metrique?: Metrique;
    dateDebut?: string;
    dateFin?: string;
  };
}

const PERIODES = [
  { key: '24h', label: '24h', days: 1 },
  { key: '7j', label: '7j', days: 7 },
  { key: '30j', label: '30j', days: 30 },
  { key: '6mois', label: '6 mois', days: 180 },
  { key: '1an', label: '1 an', days: 365 },
];

export function KpiSection({ modeFiltre = 'independant', filtreGlobal }: KpiSectionProps) {
  const [kpis, setKpis] = useState<KpiResponseDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<PointMesure | null>(null);
  const [selectedMetrique, setMetrique] = useState<Metrique | null>(null);
  const [periode, setPeriode] = useState('30j');
  const [customRange, setCustomRange] = useState<DateRange | undefined>(undefined);
  const [animatedFields, setAnimatedFields] = useState<Set<AnimatedField>>(new Set());

  const { connected, subscribeToKpis } = useDashboardWebSocket();

  const { data: pointMesures } = usePointMesures();

  const isGlobalMode = modeFiltre === 'global' && filtreGlobal?.idPointMesure;

  // Set default CABINE point and TEMPERATURE metric
  useEffect(() => {
    if (pointMesures && pointMesures.length > 0 && !selectedPoint && !selectedMetrique) {
      const defaultPoint = pointMesures.find((p) => p.typeEmplacement === 'CABINE') || pointMesures[0];
      if (defaultPoint) {
        setSelectedPoint(defaultPoint);
        setMetrique('TEMPERATURE');
      }
    }
  }, [pointMesures, selectedPoint, selectedMetrique]);

  // Calcul des dates selon la période
  const getDates = useCallback(() => {
    if (periode === 'custom' && customRange?.from) {
      return {
        dateDebut: customRange.from.toISOString(),
        dateFin: (customRange.to ?? customRange.from).toISOString(),
      };
    }
    const days = PERIODES.find((p) => p.key === periode)?.days ?? 30;
    const now = new Date();
    const start = new Date(now);
    start.setDate(start.getDate() - days);
    return {
      dateDebut: start.toISOString(),
      dateFin: now.toISOString(),
    };
  }, [periode, customRange]);

  // Fetch KPIs
  const fetchKpis = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      let params: KpiParams = {};

      if (selectedPoint && selectedMetrique) {
        params = {
          pointMesureId: selectedPoint.id,
          metrique: selectedMetrique,
          ...getDates(),
        };
      }

      const data = await getKpis(params);
      setKpis(data);
    } catch (e) {
      console.error('Erreur fetch KPIs:', e);
      setError('Impossible de charger les KPIs');
    } finally {
      setLoading(false);
    }
  }, [selectedPoint, selectedMetrique, getDates]);

  // Fetch initial
  useEffect(() => {
    fetchKpis();
  }, [fetchKpis]);

  // Abonnement WebSocket
  useEffect(() => {
    const unsubscribe = subscribeToKpis((data: unknown) => {
      const kpiMessage = data as { alertesActives: number; nbPointsEnAnomalie: number };
      setKpis((prev) => {
        if (!prev) return prev;

        // Détecter les changements pour déclencher l'animation
        const changedFields = new Set<AnimatedField>();
        if (prev.alertesActives !== kpiMessage.alertesActives) {
          changedFields.add('alertesActives');
        }
        if (prev.nbPointsEnAnomalie !== kpiMessage.nbPointsEnAnomalie) {
          changedFields.add('nbPointsEnAnomalie');
        }

        // Activer l'animation sur les champs changés
        if (changedFields.size > 0) {
          setAnimatedFields(changedFields);
          // Désactiver après 500ms
          setTimeout(() => {
            setAnimatedFields(new Set());
          }, 500);
        }

        return {
          ...prev,
          alertesActives: kpiMessage.alertesActives,
          nbPointsEnAnomalie: kpiMessage.nbPointsEnAnomalie,
        };
      });
    });
    return unsubscribe;
  }, [subscribeToKpis]);

  // Sync avec filtre global
  useEffect(() => {
    if (isGlobalMode && filtreGlobal) {
      // En mode global, on utilise les valeurs du filtre global
      // Note: il faudrait récupérer le PointMesure complet via l'API
      // Pour simplifier, on laisse le filtre global gérer les IDs
    }
  }, [isGlobalMode, filtreGlobal]);

  if (error) {
    return (
      <div className="neu-card p-5">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  if (loading || !kpis) {
    return (
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="neu-card p-5 animate-pulse">
            <div className="h-4 bg-muted rounded w-1/2 mb-2" />
            <div className="h-8 bg-muted rounded w-1/3" />
          </div>
        ))}
      </div>
    );
  }

  const scopeActive = selectedPoint && selectedMetrique;

  return (
    <div className="space-y-4">
      {/* Header avec sélecteurs regroupés si mode indépendant */}
      {modeFiltre === 'independant' && (
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              KPIs
            </span>
            {connected && (
              <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            )}
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <PointMesureMetriqueSelector
              selectedPointMesure={selectedPoint}
              selectedMetrique={selectedMetrique}
              onPointMesureChange={setSelectedPoint}
              onMetriqueChange={setMetrique}
              variant="inline"
            />
            {/* Sélecteur période */}
            <div className="neu-inset flex gap-1 rounded-2xl p-1">
              {PERIODES.map((p) => (
                <button
                  key={p.key}
                  onClick={() => {
                    setPeriode(p.key);
                    setCustomRange(undefined);
                  }}
                  className={
                    'rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                    (periode === p.key
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground')
                  }
                >
                  {p.label}
                </button>
              ))}
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    onClick={() => {
                      setPeriode('custom');
                      setCustomRange(undefined); // Réinitialiser pour calendrier vide
                    }}
                    className={
                      'flex items-center gap-2 rounded-xl px-3 py-1.5 text-xs font-semibold transition-all ' +
                      (periode === 'custom'
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:text-foreground')
                    }
                  >
                    <CalendarIcon className="h-3.5 w-3.5" />
                    {periode === 'custom' && customRange?.from
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
                      if (r?.from) setPeriode('custom');
                    }}
                    locale={fr}
                    numberOfMonths={1}
                    className="p-3 pointer-events-auto"
                    modifiers={{
                      today: undefined // Désactiver la mise en évidence d'aujourd'hui
                    }}
                  />
                </PopoverContent>
              </Popover>
            </div>
          </div>
        </div>
      )}

      {/* Cartes KPI */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {/* Alertes Actives */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Alertes Actives
            </p>
            <h3
              className={`text-2xl font-bold tracking-tight text-[color:var(--danger)] ${
                animatedFields.has('alertesActives') ? 'animate-pulse' : ''
              }`}
            >
              {kpis.alertesActives}
            </h3>
            <p
              className={`text-xs text-muted-foreground ${
                animatedFields.has('nbPointsEnAnomalie') ? 'animate-pulse' : ''
              }`}
            >
              {kpis.nbPointsEnAnomalie} points en anomalie
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
          </div>
        </div>

        {/* Taux de Conformité (remplace Température moyenne) */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Taux de Conformité
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {scopeActive && kpis.tauxConformite != null
                ? `${Math.round(kpis.tauxConformite)}%`
                : kpis.nbPointsTotal > 0
                  ? `${Math.round(((kpis.nbPointsTotal - kpis.nbPointsEnAnomalie) / kpis.nbPointsTotal) * 100)}%`
                  : 'N/A'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {scopeActive && kpis.tauxConformite != null
                ? 'Sur la période sélectionnée'
                : 'Vue globale'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <CheckCircle className="h-5 w-5 text-emerald-500" />
          </div>
        </div>

        {/* Temps moyen entre incidents (remplace Humidité moyenne) */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Temps Moyen Incidents
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {scopeActive && kpis.tempsMoyenEntreIncidentsHeures != null
                ? formatDureeHeures(kpis.tempsMoyenEntreIncidentsHeures)
                : 'N/A'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {scopeActive ? 'Entre alertes SEUIL_ABSOLU' : 'Sélectionner un point'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <Clock className="h-5 w-5 text-chart-2" />
          </div>
        </div>

        {/* Temps retour normal */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Temps Retour Normal
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {scopeActive && kpis.tempsMoyenRetourNormalHeures != null
                ? formatDureeHeures(kpis.tempsMoyenRetourNormalHeures)
                : 'N/A'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {scopeActive ? 'Après résolution' : 'Sélectionner un point'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <Activity className="h-5 w-5 text-chart-3" />
          </div>
        </div>
      </div>
    </div>
  );
}
