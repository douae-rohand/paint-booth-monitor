import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';

interface UseNouvellesMesuresDisponiblesParams {
  typePoint: 'CABINE' | 'ETUVE';
  idPointMesureCabine?: number;
  idsZonesEtuve?: number[];
  zoneNamesEtuve?: string[];
  selectedZone?: string;
  page: number;
  dateFin?: string;
  onRefresh: () => void;
}

/**
 * Hook pour gérer les notifications de nouvelles mesures disponibles via WebSocket.
 * 
 * Comportement :
 * - S'abonne aux topics WebSocket appropriés selon l'onglet actif
 * - Incrémente un compteur quand de nouvelles mesures arrivent
 * - N'affiche la notification que si l'utilisateur est sur la page 0, sans filtre de date dans le passé
 * - Se désabonne automatiquement lors du changement d'onglet, zone, ou démontage
 */
export const useNouvellesMesuresDisponibles = ({
  typePoint,
  idPointMesureCabine,
  idsZonesEtuve,
  zoneNamesEtuve,
  selectedZone,
  page,
  dateFin,
  onRefresh,
}: UseNouvellesMesuresDisponiblesParams) => {
  const { subscribe, unsubscribe } = useWebSocketContext();
  const [count, setCount] = useState(0);
  const [visible, setVisible] = useState(false);
  const subscriptionsRef = useRef<Map<string, () => void>>(new Map());

  /**
   * Vérifie si les conditions sont remplies pour afficher la notification :
   * - Page 0 (vue la plus récente)
   * - Pas de filtre de date dans le passé
   */
  const shouldShowNotification = useCallback(() => {
    if (page !== 0) {
      return false;
    }

    if (dateFin) {
      const finDate = new Date(dateFin);
      const now = new Date();
      // Si la date de fin est dans le passé (avec une marge de 1 minute pour éviter les faux positifs)
      if (finDate < new Date(now.getTime() - 60000)) {
        return false;
      }
    }

    return true;
  }, [page, dateFin]);

  const handleMessage = useCallback(() => {
    const shouldShow = shouldShowNotification();
    if (shouldShow) {
      setCount(prev => prev + 1);
      setVisible(true);
    }
  }, [shouldShowNotification]);

  /**
   * Ref stable pointant toujours vers la version courante de handleMessage.
   * Permet au callback WebSocket de lire le handler le plus récent sans
   * figurer dans les dépendances du useEffect de souscription — ce qui
   * évitait le reset intempestif (setCount(0) + setVisible(false)) à chaque
   * re-render provoqué par setVisible/setCount eux-mêmes.
   */
  const handleMessageRef = useRef(handleMessage);
  useEffect(() => {
    handleMessageRef.current = handleMessage;
  }, [handleMessage]);

  /**
   * Nettoyer toutes les souscriptions
   */
  const cleanupSubscriptions = useCallback(() => {
    subscriptionsRef.current.forEach(unsubscribe => unsubscribe());
    subscriptionsRef.current.clear();
  }, [unsubscribe]);

  /**
   * Créer un objet stable des dépendances pour éviter les changements de taille du tableau
   */
  const subscriptionDeps = useMemo(() => ({
    typePoint,
    idPointMesureCabine,
    idsZonesEtuve,
    zoneNamesEtuve,
    selectedZone,
  }), [typePoint, idPointMesureCabine, idsZonesEtuve, zoneNamesEtuve, selectedZone]);

  /**
   * Gérer les souscriptions selon l'onglet actif.
   * handleMessage est intentionnellement absent des dépendances : on passe
   * un wrapper stable qui délègue à handleMessageRef.current, de sorte que
   * le changement de logique du handler ne provoque pas de désabonnement/
   * réabonnement (et donc pas de reset du banner).
   */
  useEffect(() => {
    cleanupSubscriptions();
    setCount(0);
    setVisible(false);

    const topics: string[] = [];

    if (subscriptionDeps.typePoint === 'CABINE' && subscriptionDeps.idPointMesureCabine) {
      // S'abonner à température et humidité pour la cabine
      topics.push(`/topic/mesures/${subscriptionDeps.idPointMesureCabine}/TEMPERATURE`);
      topics.push(`/topic/mesures/${subscriptionDeps.idPointMesureCabine}/HUMIDITE`);
    } else if (subscriptionDeps.typePoint === 'ETUVE' && subscriptionDeps.idsZonesEtuve && subscriptionDeps.zoneNamesEtuve) {
      if (subscriptionDeps.selectedZone && subscriptionDeps.selectedZone !== 'all') {
        // Zone spécifique : trouver l'ID correspondant au nom de zone
        const zoneIndex = subscriptionDeps.zoneNamesEtuve.findIndex(name => name === subscriptionDeps.selectedZone);
        if (zoneIndex !== -1 && subscriptionDeps.idsZonesEtuve[zoneIndex]) {
          topics.push(`/topic/mesures/${subscriptionDeps.idsZonesEtuve[zoneIndex]}/TEMPERATURE`);
        }
      } else {
        // Toutes les zones : s'abonner aux 5 zones
        subscriptionDeps.idsZonesEtuve.forEach(id => {
          topics.push(`/topic/mesures/${id}/TEMPERATURE`);
        });
      }
    }

    // Callback stable : délègue à la ref pour lire toujours le handler le plus récent
    const stableCallback = () => handleMessageRef.current();

    // S'abonner à tous les topics
    topics.forEach(topic => {
      const unsubscribe = subscribe(topic, stableCallback);
      subscriptionsRef.current.set(topic, unsubscribe);
    });

    return cleanupSubscriptions;
  }, [subscriptionDeps, subscribe, cleanupSubscriptions]);

  /**
   * Masquer la notification et réinitialiser le compteur
   */
  const dismiss = useCallback(() => {
    setVisible(false);
    setCount(0);
  }, []);

  /**
   * Rafraîchir les données et masquer la notification
   */
  const refresh = useCallback(() => {
    onRefresh();
    dismiss();
  }, [onRefresh, dismiss]);

  /**
   * Masquer la notification lors du changement de page ou de filtre
   * NOTE: Désactivé pour permettre à l'utilisateur de voir la notification même après changement
   * La notification ne se masque que lors du clic sur rafraîchir ou changement d'onglet/zone
   */
  // useEffect(() => {
  //   if (!shouldShowNotification()) {
  //     dismiss();
  //   }
  // }, [page, dateFin, shouldShowNotification, dismiss]);

  return {
    visible,
    count,
    refresh,
    dismiss,
  };
};

export default useNouvellesMesuresDisponibles;
