import { useState, useEffect } from 'react';
import { getAlertesActives, type AlerteDTO } from '@/api/alerting';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';
import { CheckCircle2, AlertTriangle, AlertCircle, Clock } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

export interface ActiveAlertsBandProps {
  alerts?: AlerteDTO[];
  loading?: boolean;
  error?: string | null;
}

export function ActiveAlertsBand({ alerts: propAlerts, loading: propLoading, error: propError }: ActiveAlertsBandProps = {}) {
  const isLiftingState = propAlerts !== undefined;
  
  const [localAlertes, setLocalAlertes] = useState<AlerteDTO[]>([]);
  const [localLoading, setLocalLoading] = useState(true);
  const [localError, setLocalError] = useState<string | null>(null);
  
  const alertes = isLiftingState ? propAlerts : localAlertes;
  const loading = isLiftingState ? propLoading : localLoading;
  const error = isLiftingState ? propError : localError;

  const { connected, subscribeToAlertes } = useDashboardWebSocket();
  const [, setTick] = useState(0); // Force re-render every minute

  useEffect(() => {
    if (isLiftingState) return;

    const fetchAlertesActives = async () => {
      try {
        setLocalLoading(true);
        setLocalError(null);
        const data = await getAlertesActives();
        setLocalAlertes(data);
      } catch (e) {
        console.error('Erreur fetch alertes actives:', e);
        setLocalError('Impossible de charger les alertes actives');
      } finally {
        setLocalLoading(false);
      }
    };

    fetchAlertesActives();

    // Abonnement WebSocket pour les mises à jour temps réel
    const unsubscribe = subscribeToAlertes(() => {
      // Sur réception d'un message d'alerte, recharger la liste complète
      // pour garantir la cohérence des données (ajout ou retrait d'alertes)
      fetchAlertesActives();
    });

    return unsubscribe;
  }, [subscribeToAlertes, isLiftingState]);

  // Rafraîchir l'affichage des durées toutes les minutes
  useEffect(() => {
    const interval = setInterval(() => {
      setTick((prev) => prev + 1);
    }, 60000); // 60 secondes

    return () => clearInterval(interval);
  }, []);

  const formatDuree = (dateCreation: string): string => {
    const now = new Date();
    const creation = new Date(dateCreation);
    const diffMs = now.getTime() - creation.getTime();
    const diffMinutes = Math.floor(diffMs / 60000);

    if (diffMinutes < 60) {
      return `${diffMinutes} min`;
    }
    const hours = Math.floor(diffMinutes / 60);
    const remainingMinutes = diffMinutes % 60;
    if (remainingMinutes === 0) {
      return `${hours}h`;
    }
    return `${hours}h ${remainingMinutes}min`;
  };

  const getSeverityIcon = (severite: string) => {
    switch (severite) {
      case 'CRITIQUE':
        return <AlertCircle className="h-4 w-4" />;
      case 'MOYENNE':
        return <AlertTriangle className="h-4 w-4" />;
      default:
        return <Clock className="h-4 w-4" />;
    }
  };

  const getSeverityColor = (severite: string): string => {
    switch (severite) {
      case 'CRITIQUE':
        return 'bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300 border border-rose-200/60 dark:border-rose-900/40 font-semibold shadow-none';
      case 'MOYENNE':
        return 'bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300 border border-amber-200/60 dark:border-amber-900/40 font-semibold shadow-none';
      default:
        return 'bg-slate-50 text-slate-700 dark:bg-slate-950/30 dark:text-slate-300 border border-slate-200/60 dark:border-slate-800/40 font-semibold shadow-none';
    }
  };

  if (error) {
    return (
      <div className="neu-card p-6 mb-6">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="neu-card p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-bold">Alertes actives</h3>
          <div className="h-6 w-12 bg-muted rounded animate-pulse" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="animate-pulse">
              <div className="h-4 bg-muted rounded w-2/3 mb-2" />
              <div className="h-3 bg-muted rounded w-1/2 mb-2" />
              <div className="h-2 bg-muted rounded w-1/3" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="mb-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-bold">
          Alertes actives ({alertes.length})
        </h3>
      </div>

      {alertes.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/10 mb-4">
            <CheckCircle2 className="h-8 w-8 text-emerald-500" />
          </div>
          <p className="text-sm font-medium text-foreground">Aucune alerte active</p>
          <p className="text-xs text-muted-foreground mt-1">Toutes les mesures sont dans les normes</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {alertes.map((alerte) => (
            <div
              key={alerte.idAlerte}
              className="rounded-2xl border border-border/60 bg-[color:var(--surface)] p-4 shadow-[5px_5px_15px_rgba(174,174,192,0.18),-5px_-5px_15px_rgba(255,255,255,0.7)] transition-shadow hover:shadow-[7px_7px_20px_rgba(174,174,192,0.25),-7px_-7px_20px_rgba(255,255,255,0.85)]"
            >
              <div className="flex items-start justify-between mb-2">
                <div className="flex-1">
                  <p className="text-sm font-semibold">{alerte.pointMesureNom}</p>
                  <p className="text-xs text-muted-foreground">{alerte.metrique}</p>
                </div>
                <Badge className={getSeverityColor(alerte.severite)}>
                  <div className="flex items-center gap-1">
                    {getSeverityIcon(alerte.severite)}
                    <span className="text-xs">{alerte.severite}</span>
                  </div>
                </Badge>
              </div>
              <div className="flex items-center justify-between mt-3">
                <span className="text-xs text-muted-foreground">{alerte.typeAlerte}</span>
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Clock className="h-3 w-3" />
                  <span>{formatDuree(alerte.dateCreation)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
