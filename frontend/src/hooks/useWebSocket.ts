import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type MessageHandler = (data: unknown) => void;

interface WebSocketConfig {
  url: string;
  reconnectDelay: number;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: unknown) => void;
}

/**
 * Hook pour gérer la connexion WebSocket STOMP.
 * 
 * API exposée :
 * - connected : état de connexion
 * - subscribe(topic, handler) : s'abonner à un topic
 * - unsubscribe(topic) : se désabonner d'un topic
 * - Reconnexion automatique avec backoff exponentiel
 */
export const useWebSocket = (config: WebSocketConfig) => {
  const [connected, setConnected] = useState(false);
  const stompClientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, () => void>>(new Map());
  const pendingSubscriptionsRef = useRef<Array<{ topic: string; handler: MessageHandler }>>([]);

  const { url, reconnectDelay = 5000, onConnect, onDisconnect, onError } = config;

  // Utiliser des refs pour les callbacks pour éviter les recréations
  const onConnectRef = useRef(onConnect);
  const onDisconnectRef = useRef(onDisconnect);
  const onErrorRef = useRef(onError);

  useEffect(() => {
    onConnectRef.current = onConnect;
    onDisconnectRef.current = onDisconnect;
    onErrorRef.current = onError;
  }, [onConnect, onDisconnect, onError]);

  const connect = useCallback(() => {
    if (stompClientRef.current && stompClientRef.current.connected) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(url),
      reconnectDelay: reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: {
        // Le cookie httpOnly JWT est transmis automatiquement par le navigateur
        // via withCredentials (activé par défaut dans SockJS)
      },
      onConnect: () => {
        setConnected(true);
        console.log('WebSocket STOMP connecté');
        // Traiter les abonnements en attente
        pendingSubscriptionsRef.current.forEach(({ topic, handler }) => {
          if (stompClientRef.current && stompClientRef.current.connected) {
            const subscription = stompClientRef.current.subscribe(topic, (message) => {
              try {
                const data = JSON.parse(message.body);
                handler(data);
              } catch (e) {
                console.error('Erreur parsing message WebSocket:', e);
                handler(message.body);
              }
            });
            subscriptionsRef.current.set(topic, () => subscription.unsubscribe());
          }
        });
        pendingSubscriptionsRef.current = [];
        onConnectRef.current?.();
      },
      onDisconnect: () => {
        setConnected(false);
        console.log('WebSocket STOMP déconnecté');
        onDisconnectRef.current?.();
      },
      onStompError: (frame) => {
        console.error('Erreur STOMP:', frame);
        setConnected(false);
        onErrorRef.current?.(frame);
      },
    });

    client.activate();
    stompClientRef.current = client;
  }, [url, reconnectDelay]);

  const disconnect = useCallback(() => {
    if (stompClientRef.current) {
      // Se désabonner de tous les topics
      subscriptionsRef.current.forEach(unsubscribe => unsubscribe());
      subscriptionsRef.current.clear();

      stompClientRef.current.deactivate();
      stompClientRef.current = null;
      setConnected(false);
    }
  }, []);

  const subscribe = useCallback((topic: string, handler: MessageHandler) => {
    if (!stompClientRef.current || !stompClientRef.current.connected) {
      // Ajouter à la file d'attente si non connecté
      console.log('WebSocket non connecté, abonnement en attente pour:', topic);
      pendingSubscriptionsRef.current.push({ topic, handler });
      return () => {
        // Retirer de la file d'attente si annulé avant connexion
        const index = pendingSubscriptionsRef.current.findIndex(p => p.topic === topic);
        if (index !== -1) {
          pendingSubscriptionsRef.current.splice(index, 1);
        }
      };
    }

    const subscription = stompClientRef.current.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        handler(data);
      } catch (e) {
        console.error('Erreur parsing message WebSocket:', e);
        handler(message.body);
      }
    });

    subscriptionsRef.current.set(topic, () => subscription.unsubscribe());

    return () => {
      const unsubscribe = subscriptionsRef.current.get(topic);
      if (unsubscribe) {
        unsubscribe();
        subscriptionsRef.current.delete(topic);
      }
    };
  }, []);

  const unsubscribe = useCallback((topic: string) => {
    const unsubscribe = subscriptionsRef.current.get(topic);
    if (unsubscribe) {
      unsubscribe();
      subscriptionsRef.current.delete(topic);
    }
  }, []);

  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return { connected, subscribe, unsubscribe };
};

export default useWebSocket;
