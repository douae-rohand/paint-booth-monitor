import { useRef, useCallback } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';

/**
 * Hook centralisé pour le WebSocket du dashboard.
 * Utilise le Context pour partager une seule connexion WebSocket entre tous les composants.
 * 
 * Expose des méthodes pour s'abonner dynamiquement aux topics variables :
 * - subscribeToMesures(idPointMesure, metrique, handler)
 * - unsubscribeFromMesures(idPointMesure, metrique)
 */
export const useDashboardWebSocket = () => {
  const { connected, subscribe, unsubscribe } = useWebSocketContext();

  const subscriptionsRef = useRef<Map<string, () => void>>(new Map());

  /**
   * S'abonner au topic des mesures pour un point et une métrique donnés.
   * Topic : /topic/mesures/{idPointMesure}/{metrique}
   */
  const subscribeToMesures = useCallback((
    idPointMesure: number,
    metrique: string,
    handler: (data: unknown) => void
  ) => {
    const topic = `/topic/mesures/${idPointMesure}/${metrique}`;
    const unsubscribe = subscribe(topic, handler);
    subscriptionsRef.current.set(topic, unsubscribe);
    return unsubscribe;
  }, [subscribe]);

  /**
   * Se désabonner du topic des mesures pour un point et une métrique donnés.
   */
  const unsubscribeFromMesures = useCallback((idPointMesure: number, metrique: string) => {
    const topic = `/topic/mesures/${idPointMesure}/${metrique}`;
    const unsubscribe = subscriptionsRef.current.get(topic);
    if (unsubscribe) {
      unsubscribe();
      subscriptionsRef.current.delete(topic);
    }
  }, []);

  /**
   * S'abonner au topic du statut temps réel.
   * Topic : /topic/statut-temps-reel
   */
  const subscribeToStatutTempsReel = useCallback((handler: (data: unknown) => void) => {
    const topic = '/topic/statut-temps-reel';
    const unsubscribe = subscribe(topic, handler);
    subscriptionsRef.current.set(topic, unsubscribe);
    return unsubscribe;
  }, [subscribe]);

  /**
   * S'abonner au topic des KPIs.
   * Topic : /topic/kpis
   */
  const subscribeToKpis = useCallback((handler: (data: unknown) => void) => {
    const topic = '/topic/kpis';
    const unsubscribe = subscribe(topic, handler);
    subscriptionsRef.current.set(topic, unsubscribe);
    return unsubscribe;
  }, [subscribe]);

  /**
   * S'abonner au topic des alertes.
   * Topic : /topic/alertes
   */
  const subscribeToAlertes = useCallback((handler: (data: unknown) => void) => {
    const topic = '/topic/alertes';
    const unsubscribe = subscribe(topic, handler);
    subscriptionsRef.current.set(topic, unsubscribe);
    return unsubscribe;
  }, [subscribe]);

  return {
    connected,
    subscribeToMesures,
    unsubscribeFromMesures,
    subscribeToStatutTempsReel,
    subscribeToKpis,
    subscribeToAlertes,
  };
};

export default useDashboardWebSocket;
