import { useState, useEffect, useCallback, useRef } from 'react';
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
  { key: '24h',   label: '24h',    days: 1   },
  { key: '7j',    label: '7j',     days: 7   },
  { key: '30j',   label: '30j',    days: 30  },
  { key: '6mois', label: '6 mois', days: 180 },
  { key: '1an',   label: '1 an',   days: 365 },
];

export function KpiSection({ modeFiltre = 'independant', filtreGlobal }: KpiSectionProps) {
  const [kpis, setKpis]               = useState<KpiResponseDTO | null>(null);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState<string | null>(null);
  const [selectedPoint, setSelectedPoint]   = useState<PointMesure | null>(null);
  const [selectedMetrique, setMetrique]     = useState<Metrique | null>(null);
  const [periode, setPeriode]         = useState('30j');
  const [customRange, setCustomRange] = useState<DateRange | undefined>(undefined);

  // ── Protection anti-chevauchement ────────────────────────────────────────
  // fetchInProgress : empêche deux fetchs concurrents déclenchés par des
  //   signaux WebSocket rapprochés.
  // pendingRefetch  : si un signal arrive PENDANT un fetch, on arme un seul
  //   re-fetch supplémentaire à exécuter juste après la fin du fetch en cours.
  // debounceTimer   : absorbe plusieurs signaux consécutifs rapides (ex : cascade
  //   de résolutions d'alertes) en un seul appel réseau après 400 ms de silence.
  const fetchInProgress = useRef(false);
  const pendingRefetch  = useRef(false);
  const debounceTimer   = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isMounted       = useRef(true);

  const { connected, subscribeToAlertes } = useDashboardWebSocket();
  const { data: pointMesures } = usePointMesures();

  const isGlobalMode = modeFiltre === 'global' && filtreGlobal?.idPointMesure;

  // Nettoyage au démontage — évite tout setState après unmount
  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, []);

  // Valeur par défaut : premier point CABINE + TEMPERATURE
  useEffect(() => {
    if (pointMesures && pointMesures.length > 0 && !selectedPoint && !selectedMetrique) {
      const defaultPoint =
        pointMesures.find((p) => p.typeEmplacement === 'CABINE') || pointMesures[0];
      if (defaultPoint) {
        setSelectedPoint(defaultPoint);
        setMetrique('TEMPERATURE');
      }
    }
  }, [pointMesures, selectedPoint, selectedMetrique]);

  // Calcul des dates selon la période sélectionnée
  const getDates = useCallback(() => {
    if (periode === 'custom' && customRange?.from) {
      return {
        dateDebut: customRange.from.toISOString(),
        dateFin: (customRange.to ?? customRange.from).toISOString(),
      };
    }
    const days = PERIODES.find((p) => p.key === periode)?.days ?? 30;
    const now   = new Date();
    const start = new Date(now);
    start.setDate(start.getDate() - days);
    return {
      dateDebut: start.toISOString(),
      dateFin:   now.toISOString(),
    };
  }, [periode, customRange]);

  // ── Fetch KPIs (avec gestion anti-chevauchement) ──────────────────────────
  const fetchKpis = useCallback(async () => {
    // Si un fetch est déjà en cours, armer un refetch pour après
    if (fetchInProgress.current) {
      pendingRefetch.current = true;
      return;
    }

    fetchInProgress.current = true;
    pendingRefetch.current  = false;

    try {
      if (isMounted.current) {
        setLoading(true);
        setError(null);
      }

      let params: KpiParams = {};
      if (selectedPoint && selectedMetrique) {
        params = {
          pointMesureId: selectedPoint.id,
          metrique: selectedMetrique,
          ...getDates(),
        };
      }

      const data = await getKpis(params);
      if (isMounted.current) setKpis(data);
    } catch (e) {
      console.error('Erreur fetch KPIs:', e);
      if (isMounted.current) setError('Impossible de charger les KPIs');
    } finally {
      fetchInProgress.current = false;
      if (isMounted.current) setLoading(false);

      // Si un signal est arrivé pendant ce fetch, lancer un seul re-fetch
      if (pendingRefetch.current && isMounted.current) {
        pendingRefetch.current = false;
        fetchKpis();
      }
    }
  }, [selectedPoint, selectedMetrique, getDates]);

  // Fetch initial + re-fetch quand le scope ou la période change
  useEffect(() => {
    fetchKpis();
  }, [fetchKpis]);

  // ── Souscription WebSocket : /topic/alertes (même pattern qu'ActiveAlertsBand)
  // Le message WebSocket sert uniquement de signal de changement.
  // Son contenu n'est jamais lu ni appliqué directement.
  // Un debounce de 400 ms absorbe les cascades de résolutions rapprochées.
  useEffect(() => {
    const unsubscribe = subscribeToAlertes(() => {
      // Annuler le timer précédent si un nouveau signal arrive dans les 400 ms
      if (debounceTimer.current) clearTimeout(debounceTimer.current);

      debounceTimer.current = setTimeout(() => {
        if (isMounted.current) fetchKpis();
      }, 400);
    });

    return () => {
      unsubscribe();
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [subscribeToAlertes, fetchKpis]);

  // Sync avec filtre global (réservé au mode global, non utilisé en mode indépendant)
  useEffect(() => {
    if (isGlobalMode && filtreGlobal) {
      // Le filtre global gère les IDs en amont
    }
  }, [isGlobalMode, filtreGlobal]);

  // ── Rendu ─────────────────────────────────────────────────────────────────

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
      {/* Header avec sélecteurs — mode indépendant uniquement */}
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
                      setCustomRange(undefined);
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
                    modifiers={{ today: undefined }}
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
            <h3 className="text-2xl font-bold tracking-tight text-[color:var(--danger)]">
              {kpis.alertesActives}
            </h3>
            <p className="text-xs text-muted-foreground">
              {kpis.nbPointsEnAnomalie} {scopeActive ? 'sur ce point/métrique' : 'points en anomalie'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <AlertTriangle className="h-5 w-5 text-[color:var(--danger)]" />
          </div>
        </div>

        {/* Taux de Conformité */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Taux de Conformité
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {kpis.tauxConformite != null
                ? `${Math.round(kpis.tauxConformite)}%`
                : scopeActive
                  ? '--'
                  : kpis.nbPointsTotal > 0
                    ? `${Math.round(((kpis.nbPointsTotal - kpis.nbPointsEnAnomalie) / kpis.nbPointsTotal) * 100)}%`
                    : '--'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {kpis.tauxConformite != null
                ? 'Sur la période sélectionnée'
                : scopeActive
                  ? 'Aucun seuil configuré'
                  : 'Vue globale'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <CheckCircle className="h-5 w-5 text-emerald-500" />
          </div>
        </div>

        {/* Temps Moyen Incidents */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Temps Moyen Incidents
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {scopeActive && kpis.tempsMoyenEntreIncidentsHeures != null
                ? formatDureeHeures(kpis.tempsMoyenEntreIncidentsHeures)
                : '--'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {scopeActive ? 'Entre alertes SEUIL_ABSOLU' : 'Sélectionnez un point de mesure'}
            </p>
          </div>
          <div className="neu-pressable p-3 rounded-2xl">
            <Clock className="h-5 w-5 text-chart-2" />
          </div>
        </div>

        {/* Temps Retour Normal */}
        <div className="neu-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Temps Retour Normal
            </p>
            <h3 className="text-2xl font-bold tracking-tight text-foreground">
              {scopeActive && kpis.tempsMoyenRetourNormalHeures != null
                ? formatDureeHeures(kpis.tempsMoyenRetourNormalHeures)
                : '--'}
            </h3>
            <p className="text-xs text-muted-foreground">
              {scopeActive ? 'Après résolution' : 'Sélectionnez un point de mesure'}
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
