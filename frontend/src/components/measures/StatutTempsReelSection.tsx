import { useState, useEffect, useCallback, useRef } from 'react';
import { ChevronDown, Thermometer, Droplets } from 'lucide-react';
import { getStatutTempsReel, type PointMesureStatutDTO, type StatutMesure } from '@/api/measures';
import { useDashboardWebSocket } from '@/hooks/useDashboardWebSocket';

const STATUT_COLORS: Record<StatutMesure, string> = {
  CRITIQUE: 'var(--danger)',
  ATTENTION: 'var(--warning)',
  NOMINAL: 'var(--success)',
  INCONNU: 'var(--muted-foreground)',
};

const STATUT_LABELS: Record<StatutMesure, string> = {
  CRITIQUE: 'Critique',
  ATTENTION: 'Attention',
  NOMINAL: 'Nominal',
  INCONNU: 'Pas de données récentes',
};

export function StatutTempsReelSection() {
  const [statuts, setStatuts] = useState<PointMesureStatutDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { connected, subscribeToStatutTempsReel } = useDashboardWebSocket();

  const pollingRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch initial
  const fetchStatuts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getStatutTempsReel();
      setStatuts(data);
    } catch (e) {
      console.error('Erreur fetch statut temps réel:', e);
      setError('Impossible de charger le statut temps réel');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatuts();
  }, [fetchStatuts]);

  // Polling fallback si WebSocket déconnecté
  useEffect(() => {
    if (!connected) {
      pollingRef.current = setInterval(fetchStatuts, 15000);
    } else {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    }
    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
      }
    };
  }, [connected, fetchStatuts]);

  // Abonnement WebSocket
  useEffect(() => {
    const unsubscribe = subscribeToStatutTempsReel((data: unknown) => {
      console.log('StatutTempsReel: Message WebSocket reçu', data);
      const message = data as PointMesureStatutDTO;
      setStatuts((prev) => {
        const index = prev.findIndex((s) => s.idPointMesure === message.idPointMesure);
        if (index >= 0) {
          const updated = [...prev];
          updated[index] = message;
          console.log('StatutTempsReel: Statut mis à jour pour point', message.idPointMesure);
          return updated;
        }
        console.log('StatutTempsReel: Point non trouvé dans la liste', message.idPointMesure);
        return prev;
      });
    });
    return unsubscribe;
  }, [subscribeToStatutTempsReel]);

  const toggleExpanded = (id: number) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

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
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-bold">Statut temps réel</h3>
          <span className="text-xs text-muted-foreground">Chargement...</span>
        </div>
        <div className="space-y-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-12 bg-muted rounded-2xl animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="neu-card p-6">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-bold">Statut temps réel</h3>
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground">
            {connected ? 'Live' : 'Hors ligne'}
          </span>
          <span
            className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-500 animate-pulse' : 'bg-muted-foreground'}`}
          />
        </div>
      </div>
      <ul className="mt-4 space-y-2">
        {statuts.map((point) => {
          const isExpanded = expandedId === point.idPointMesure;
          return (
            <li key={point.idPointMesure}>
              <button
                onClick={() => toggleExpanded(point.idPointMesure)}
                className="w-full flex items-center justify-between rounded-2xl px-3 py-2.5 transition-all hover:bg-muted/40"
                style={{ boxShadow: 'var(--shadow-neu-inset)' }}
              >
                <div className="flex items-center gap-3">
                  <span
                    className="h-2.5 w-2.5 rounded-full flex-shrink-0"
                    style={{
                      background: point.mesures.length > 0
                        ? STATUT_COLORS[
                            point.mesures.some((m) => m.statut === 'CRITIQUE')
                              ? 'CRITIQUE'
                              : point.mesures.some((m) => m.statut === 'ATTENTION')
                                ? 'ATTENTION'
                                : 'NOMINAL'
                          ]
                        : STATUT_COLORS.INCONNU,
                      ...(point.mesures.some((m) => m.statut === 'CRITIQUE') && { className: 'h-2.5 w-2.5 rounded-full flex-shrink-0 pulse-dot' }),
                    }}
                  />
                  <div className="text-left">
                    <p className="text-sm font-semibold">{point.nomPointMesure}</p>
                    <p className="text-xs text-muted-foreground">
                      {point.typeEmplacement === 'CABINE' ? 'Cabine' : 'Étuve'}
                    </p>
                  </div>
                </div>
                <ChevronDown
                  className={`h-4 w-4 text-muted-foreground transition-transform duration-200 ${
                    isExpanded ? 'rotate-180' : ''
                  }`}
                />
              </button>

              {isExpanded && (
                <div
                  className="mt-1 mx-1 rounded-xl px-4 py-3 space-y-2"
                  style={{ boxShadow: 'var(--shadow-neu-sm)' }}
                >
                  {point.mesures.map((mesure) => (
                    <div key={mesure.metrique}>
                      <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2">
                          {mesure.metrique === 'TEMPERATURE' ? (
                            <Thermometer className="h-3 w-3" />
                          ) : (
                            <Droplets className="h-3 w-3" />
                          )}
                          <span className="text-muted-foreground font-medium">
                            {mesure.metrique === 'TEMPERATURE' ? 'Température' : 'Humidité'}
                          </span>
                        </div>
                        <span
                          className="font-bold"
                          style={{ color: STATUT_COLORS[mesure.statut] }}
                        >
                          {mesure.derniereValeur != null
                            ? `${mesure.derniereValeur}${mesure.metrique === 'TEMPERATURE' ? ' °C' : ' %'}`
                            : 'Aucune mesure'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px] text-muted-foreground mt-0.5">
                        <span>{STATUT_LABELS[mesure.statut]}</span>
                        {mesure.dateDerniereMesure && (
                          <span>
                            {new Date(mesure.dateDerniereMesure).toLocaleTimeString('fr-FR', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        )}
                      </div>
                      <div className="h-px bg-border/50 mt-2" />
                    </div>
                  ))}
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
