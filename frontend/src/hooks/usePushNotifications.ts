/**
 * usePushNotifications — gestion du canal Web Push (VAPID).
 *
 * Ce hook est DISTINCT de useNotifications (canal IN_APP/WebSocket).
 * Les deux canaux coexistent indépendamment.
 *
 * Cycle de vie :
 *  1. Au montage (si authentifié) : enregistre le Service Worker sw-push.js
 *     et lit l'abonnement actif éventuel depuis pushManager.
 *  2. activerPush() : demande la permission, s'abonne, notifie le backend.
 *  3. desactiverPush() : désabonne côté navigateur, notifie le backend.
 *  4. À la déconnexion : NE PAS désabonner — l'abonnement reste valide
 *     pour recevoir des pushes même navigateur fermé. Juste réinitialiser l'état local.
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { getClePubliqueVapid, creerAbonnement, supprimerAbonnement } from '@/api/push';

// ── Types ─────────────────────────────────────────────────────────────────────

export type PermissionPush = 'non_supporte' | 'non_demande' | 'accorde' | 'refuse';

export interface UsePushNotificationsReturn {
  /** Le navigateur supporte-t-il Web Push ? */
  supporte: boolean;
  /** État de la permission navigateur */
  permission: PermissionPush;
  /** Un abonnement push est-il actuellement actif ? */
  abonneActif: boolean;
  /** Opération en cours (activation/désactivation) */
  chargement: boolean;
  /** Message d'erreur éventuel */
  erreur: string | null;
  /** Demande la permission et s'abonne (appel explicite utilisateur) */
  activerPush: () => Promise<void>;
  /** Désabonne le navigateur et notifie le backend */
  desactiverPush: () => Promise<void>;
}

// ── Utilitaire : convertit la clé VAPID Base64Url en Uint8Array ──────────────

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

// ── Détection du support navigateur ──────────────────────────────────────────

function isPushSupporte(): boolean {
  return (
    typeof window !== 'undefined' &&
    'Notification' in window &&
    'PushManager' in window &&
    'serviceWorker' in navigator
  );
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function usePushNotifications(isAuthenticated: boolean): UsePushNotificationsReturn {
  const supporte = isPushSupporte();

  const [permission, setPermission] = useState<PermissionPush>(() => {
    if (!supporte) return 'non_supporte';
    const p = Notification.permission;
    if (p === 'granted') return 'accorde';
    if (p === 'denied') return 'refuse';
    return 'non_demande';
  });

  const [abonneActif, setAbonneActif] = useState(false);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const swRegistrationRef = useRef<ServiceWorkerRegistration | null>(null);

  // ── Enregistrement du Service Worker + lecture abonnement existant ──────────

  useEffect(() => {
    if (!supporte || !isAuthenticated) return;

    let cancelled = false;

    const init = async () => {
      try {
        // Enregistre (ou récupère) le Service Worker sw-push.js
        const registration = await navigator.serviceWorker.register('/sw-push.js', {
          scope: '/',
        });
        if (cancelled) return;

        swRegistrationRef.current = registration;

        // Lit l'abonnement actif éventuel (existait avant ce montage)
        const existingSubscription = await registration.pushManager.getSubscription();
        if (cancelled) return;

        if (existingSubscription) {
          setAbonneActif(true);
          setPermission('accorde');
        }
      } catch (e) {
        if (!cancelled) {
          console.warn('[usePushNotifications] Erreur initialisation SW:', e);
        }
      }
    };

    init();

    return () => {
      cancelled = true;
    };
  }, [supporte, isAuthenticated]);

  // ── Synchronise la permission si elle change hors de l'app ─────────────────

  useEffect(() => {
    if (!supporte) return;
    // navigator.permissions.query permet de détecter un changement de permission
    // sans interaction utilisateur (ex: révoqué depuis les paramètres du navigateur)
    if (!navigator.permissions) return;
    let permStatus: PermissionStatus | null = null;

    navigator.permissions.query({ name: 'notifications' as PermissionName }).then((ps) => {
      permStatus = ps;
      ps.onchange = () => {
        if (ps.state === 'granted') setPermission('accorde');
        else if (ps.state === 'denied') {
          setPermission('refuse');
          setAbonneActif(false);
        } else {
          setPermission('non_demande');
          setAbonneActif(false);
        }
      };
    }).catch(() => { /* permissions API non supportée */ });

    return () => {
      if (permStatus) permStatus.onchange = null;
    };
  }, [supporte]);

  // ── activerPush ───────────────────────────────────────────────────────────

  const activerPush = useCallback(async () => {
    if (!supporte) return;
    setErreur(null);
    setChargement(true);

    try {
      // 1. Demander la permission (uniquement si non encore accordée)
      const permissionResult = await Notification.requestPermission();
      if (permissionResult !== 'granted') {
        setPermission(permissionResult === 'denied' ? 'refuse' : 'non_demande');
        return;
      }
      setPermission('accorde');

      // 2. S'assurer que le SW est enregistré
      let registration = swRegistrationRef.current;
      if (!registration) {
        registration = await navigator.serviceWorker.register('/sw-push.js', { scope: '/' });
        swRegistrationRef.current = registration;
      }

      // 3. Récupérer la clé VAPID publique depuis le backend
      const clePublique = await getClePubliqueVapid();
      const clePubliqueStr = typeof clePublique === 'string'
        ? clePublique
        : (clePublique as unknown as { data: string }).data ?? String(clePublique);

      // 4. S'abonner via PushManager
      const keyBytes = urlBase64ToUint8Array(clePubliqueStr);
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: keyBytes,
      } as PushSubscriptionOptionsInit);

      // 5. Extraire les clés du PushSubscription
      const rawKeys = subscription.toJSON().keys;
      if (!rawKeys?.p256dh || !rawKeys?.auth) {
        throw new Error('Clés de chiffrement absentes dans PushSubscription');
      }

      // 6. Envoyer l'abonnement au backend
      await creerAbonnement({
        endpoint: subscription.endpoint,
        cleP256dh: rawKeys.p256dh,
        cleAuth: rawKeys.auth,
        userAgent: navigator.userAgent,
      });

      setAbonneActif(true);
    } catch (e: unknown) {
      console.error('[usePushNotifications] Erreur activation push:', e);
      setErreur('Impossible d\'activer les notifications push. Réessayez.');
    } finally {
      setChargement(false);
    }
  }, [supporte]);

  // ── desactiverPush ────────────────────────────────────────────────────────

  const desactiverPush = useCallback(async () => {
    if (!supporte) return;
    setErreur(null);
    setChargement(true);

    try {
      const registration = swRegistrationRef.current
        ?? await navigator.serviceWorker.getRegistration('/sw-push.js');

      if (!registration) {
        setAbonneActif(false);
        return;
      }

      const subscription = await registration.pushManager.getSubscription();
      if (!subscription) {
        setAbonneActif(false);
        return;
      }

      const endpoint = subscription.endpoint;

      // 1. Désabonner côté navigateur
      await subscription.unsubscribe();

      // 2. Notifier le backend
      await supprimerAbonnement(endpoint);

      setAbonneActif(false);
    } catch (e: unknown) {
      console.error('[usePushNotifications] Erreur désactivation push:', e);
      setErreur('Impossible de désactiver les notifications push.');
    } finally {
      setChargement(false);
    }
  }, [supporte]);

  return {
    supporte,
    permission,
    abonneActif,
    chargement,
    erreur,
    activerPush,
    desactiverPush,
  };
}
