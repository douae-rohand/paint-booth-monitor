/**
 * useNotifications — centralise :
 *  - Abonnement WebSocket personnel /user/queue/notifications
 *  - Compteur non-lu (badge bell)
 *  - Liste des notifications récentes
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
  /** Liste des notifications récentes (chargée via REST au montage) */
  notifications: NotificationInAppDTO[];
  /** Compteur non-lus pour le badge */
  nonLuesCount: number;
  /** Dernière notification reçue via WebSocket (pour le toast) */
  dernierePush: NotificationInAppDTO | null;
  /** true pendant le premier chargement REST */
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
  // Évite une double requête si le composant remonte rapidement
  const loadedRef = useRef(false);

  // ── Chargement initial REST ────────────────────────────────────────────────

  const recharger = useCallback(async () => {
    try {
      const [page, count] = await Promise.all([
        getNotifications(0, 20),
        getNonLuesCount(),
      ]);
      setNotifications(page.content);
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
      // Ajouter en tête de liste sans recharger depuis le serveur
      setNotifications((prev) => [notif, ...prev]);
      setNonLuesCount((prev) => prev + 1);
      // Exposer pour le toast (réinitialisé après affichage via acquitterPush)
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
