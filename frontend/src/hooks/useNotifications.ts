/**
 * useNotifications — centralise :
 *  - Abonnement WebSocket personnel /user/queue/notifications
 *  - Compteur non-lu (badge bell)
 *  - Liste combinée (toutes les non-lues + lues récentes) chargée une fois au montage
 *  - Actions marquerLu / marquerToutLu
 *
 * Utilisé par BellNotifications (badge + panel) ET NotificationToast (toast transitoire).
 * Un seul abonnement WebSocket, partagé via ce hook.
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { useWebSocketContext } from '@/contexts/WebSocketContext';
import {
  getNotifications,
  getNonLuesCount,
  marquerLu as apiMarquerLu,
  marquerToutLu as apiMarquerToutLu,
  type NotificationInAppDTO,
} from '@/api/notifications';

const USER_QUEUE = '/user/queue/notifications';

export interface UseNotificationsReturn {
  /**
   * Liste combinée : toutes les non-lues + lues récentes (7j, max 5).
   * Le backend garantit que toutes les non-lues sont présentes.
   */
  notifications: NotificationInAppDTO[];
  /** Compteur non-lus pour le badge — piloté uniquement par marquerLu/marquerToutLu/WebSocket */
  nonLuesCount: number;
  /** Dernière notification reçue via WebSocket (pour le toast) */
  dernierePush: NotificationInAppDTO | null;
  /** true pendant le chargement initial REST */
  loading: boolean;
  /** Recharge la liste depuis le serveur */
  recharger: () => Promise<void>;
  /** Marque une notification comme lue + met à jour l'état local */
  marquerLu: (idEnvoi: string) => Promise<void>;
  /** Marque tout comme lu + met à jour l'état local */
  marquerToutLu: () => Promise<void>;
  /** Réinitialise dernierePush (appelé par le toast après affichage) */
  acquitterPush: () => void;
}

export function useNotifications(): UseNotificationsReturn {
  const { subscribe } = useWebSocketContext();
  const [notifications, setNotifications] = useState<NotificationInAppDTO[]>([]);
  const [nonLuesCount, setNonLuesCount] = useState(0);
  const [dernierePush, setDernierePush] = useState<NotificationInAppDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const loadedRef = useRef(false);

  // ── Chargement initial REST ────────────────────────────────────────────────

  const recharger = useCallback(async () => {
    try {
      const [liste, count] = await Promise.all([
        getNotifications(),
        getNonLuesCount(),
      ]);
      setNotifications(liste);
      setNonLuesCount(count);
    } catch (e) {
      console.error('[useNotifications] Erreur chargement:', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (loadedRef.current) return;
    loadedRef.current = true;
    recharger();
  }, [recharger]);

  // ── Abonnement WebSocket personnel ────────────────────────────────────────

  useEffect(() => {
    const unsubscribe = subscribe(USER_QUEUE, (data: unknown) => {
      const notif = data as NotificationInAppDTO;
      // Prépend en tête de liste sans rechargement réseau
      setNotifications((prev) => [notif, ...prev]);
      setNonLuesCount((prev) => prev + 1);
      setDernierePush(notif);
    });
    return unsubscribe;
  }, [subscribe]);

  // ── Actions ───────────────────────────────────────────────────────────────

  const marquerLu = useCallback(async (idEnvoi: string) => {
    try {
      await apiMarquerLu(idEnvoi);
      setNotifications((prev) =>
        prev.map((n) =>
          n.idEnvoi === idEnvoi
            ? { ...n, lu: true, dateLecture: new Date().toISOString() }
            : n
        )
      );
      setNonLuesCount((prev) => Math.max(0, prev - 1));
    } catch (e) {
      console.error('[useNotifications] Erreur marquerLu:', e);
    }
  }, []);

  const marquerToutLu = useCallback(async () => {
    try {
      await apiMarquerToutLu();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, lu: true, dateLecture: new Date().toISOString() }))
      );
      setNonLuesCount(0);
    } catch (e) {
      console.error('[useNotifications] Erreur marquerToutLu:', e);
    }
  }, []);

  const acquitterPush = useCallback(() => {
    setDernierePush(null);
  }, []);

  return {
    notifications,
    nonLuesCount,
    dernierePush,
    loading,
    recharger,
    marquerLu,
    marquerToutLu,
    acquitterPush,
  };
}
