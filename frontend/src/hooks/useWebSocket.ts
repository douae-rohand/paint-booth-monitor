import { useEffect, useRef, useState, useCallback } from 'react';
import { createWebSocketConnection } from '../api/ws';

type WebSocketMessage = string | object;
type MessageHandler = (data: unknown) => void;

export const useWebSocket = (onMessageReceived?: MessageHandler) => {
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const connect = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      return;
    }

    wsRef.current = createWebSocketConnection(
      (data) => {
        if (onMessageReceived) {
          onMessageReceived(data);
        }
      },
      (error) => {
        console.error('WebSocket Error:', error);
        setConnected(false);
      }
    );

    wsRef.current.onopen = () => {
      setConnected(true);
      console.log('WebSocket connection established.');
    };

    wsRef.current.onclose = () => {
      setConnected(false);
      console.log('WebSocket connection closed. Attempting reconnect in 5s...');
      reconnectTimeoutRef.current = setTimeout(() => {
        connect();
      }, 5000);
    };
  }, [onMessageReceived]);

  useEffect(() => {
    connect();

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [connect]);

  const sendMessage = (message: WebSocketMessage) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(typeof message === 'string' ? message : JSON.stringify(message));
    } else {
      console.warn('WebSocket is not open. Message not sent:', message);
    }
  };

  return { connected, sendMessage };
};

export default useWebSocket;
