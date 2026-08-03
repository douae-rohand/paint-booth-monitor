import { useState, useEffect, useCallback } from 'react';
import { getTopAlertes, type TopAlerteDTO, type TopAlertesParams, type PeriodeAlerte } from '@/api/alertes';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import type { Metrique } from '@/api/alerting/seuils';

interface TopAlertesSectionProps {
  modeFiltre?: 'global' | 'independant';
  filtreGlobal?: {
    idPointMesure?: number;
    metrique?: Metrique;
    dateDebut?: string;
    dateFin?: string;
  };
}

const PERIODES: PeriodeAlerte[] = ['24h', '7j', '30j', '6mois', '1an'];

export function TopAlertesSection({ modeFiltre = 'independant', filtreGlobal }: TopAlertesSectionProps) {
  const [topAlertes, setTopAlertes] = useState<TopAlerteDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [periode, setPeriode] = useState<PeriodeAlerte>('30j');
  const { connected, subscribeToAlertes } = useDashboardWebSocket();

  const isGlobalMode = modeFiltre === 'global' && filtreGlobal?.idPointMesure;

  // Fetch top alertes
  const fetchTopAlertes = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params: TopAlertesParams = {
        periode,
        pointMesureId: isGlobalMode ? filtreGlobal?.idPointMesure : undefined,
        limit: 10,
      };

      const data = await getTopAlertes(params);
      setTopAlertes(data);
    } catch (e) {
      console.error('Erreur fetch top alertes:', e);
      setError('Impossible de charger les top alertes');
    } finally {
      setLoading(false);
    }
  }, [periode, isGlobalMode, filtreGlobal]);

  useEffect(() => {
    fetchTopAlertes();
  }, [fetchTopAlertes]);

  // Abonnement WebSocket
  useEffect(() => {
    const unsubscribe = subscribeToAlertes((data: unknown) => {
      const alerte = data as { idPointMesure: number; metrique: Metrique };
      setTopAlertes((prev) => {
        // Incrémenter le compteur si déjà présent
        const index = prev.findIndex(
          (a) => a.idPointMesure === alerte.idPointMesure && a.metrique === alerte.metrique
        );
        if (index >= 0) {
          const updated = [...prev];
          updated[index] = {
            ...updated[index],
            nombreDepassements: updated[index].nombreDepassements + 1,
          };
          // Retrier par nombre de dépassements
          return updated.sort((a, b) => b.nombreDepassements - a.nombreDepassements);
        }
        return prev;
      });
    });
    return unsubscribe;
  }, [subscribeToAlertes]);

  if (error) {
    return (
      <div className="neu-card p-6">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="neu-card p-6">
        <h3 className="text-base font-bold mb-4">Top métriques en alerte</h3>
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="animate-pulse">
              <div className="h-4 bg-muted rounded w-1/3 mb-2" />
              <div className="h-2 bg-muted rounded w-full" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  const maxCount = Math.max(...topAlertes.map((a) => a.nombreDepassements), 1);

  return (
    <div className="neu-card p-6">
      <div className="flex flex-wrap items-start justify-between gap-4 mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-base font-bold">Top métriques en alerte</h3>
            {connected && <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />}
          </div>
          <p className="text-sm text-muted-foreground">
            Métriques ayant dépassé leur seuil récemment
          </p>
        </div>

        {/* Sélecteur période si mode indépendant */}
        {modeFiltre === 'independant' && (
          <div className="neu-inset flex items-center gap-1 p-1 rounded-2xl">
            <Select value={periode} onValueChange={(v) => setPeriode(v as PeriodeAlerte)}>
              <SelectTrigger className="flex items-center gap-1.5 rounded-xl px-3 py-1.5 h-auto text-xs font-semibold border-0 shadow-none bg-transparent hover:text-foreground text-muted-foreground focus:ring-0 focus:ring-offset-0 pointer-events-auto cursor-pointer">
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="text-xs">
                {PERIODES.map((p) => (
                  <SelectItem key={p} value={p}>
                    {p}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </div>

      <ul className="mt-5 space-y-4">
        {topAlertes.length === 0 ? (
          <li className="text-sm text-muted-foreground text-center py-4">
            Aucune alerte sur cette période
          </li>
        ) : (
          topAlertes.map((a, idx) => {
            const pct = (a.nombreDepassements / maxCount) * 100;
            return (
              <li key={`${a.idPointMesure}-${a.metrique}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="flex h-7 w-7 items-center justify-center rounded-xl bg-primary/35 text-xs font-bold text-primary">
                      {idx + 1}
                    </span>
                    <p className="text-sm font-semibold">
                      {a.nomPointMesure} - {a.metrique === 'TEMPERATURE' ? 'Température' : 'Humidité'}
                    </p>
                  </div>
                  <span className="text-sm font-bold text-[color:var(--danger)]">
                    {a.nombreDepassements} {a.nombreDepassements <= 1 ? 'alerte' : 'alertes'}
                  </span>
                </div>
                <div className="neu-inset mt-2 h-2 overflow-hidden rounded-full">
                  <div
                    className="h-full rounded-full transition-all duration-300"
                    style={{
                      width: `${pct}%`,
                      background: 'linear-gradient(90deg, var(--primary), var(--danger))',
                    }}
                  />
                </div>
              </li>
            );
          })
        )}
      </ul>
    </div>
  );
}
