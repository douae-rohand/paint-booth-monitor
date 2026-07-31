import { createContext, useContext, ReactNode } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8081/ws';

interface WebSocketContextValue {
  connected: boolean;
  subscribe: (topic: string, handler: (data: unknown) => void) => () => void;
  unsubscribe: (topic: string) => void;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

export const WebSocketProvider = ({ children }: { children: ReactNode }) => {
  const { connected, subscribe, unsubscribe } = useWebSocket({
    url: WS_URL,
    reconnectDelay: 5000,
    onConnect: () => console.log('Dashboard WebSocket connecté'),
    onDisconnect: () => console.log('Dashboard WebSocket déconnecté'),
  });

  return (
    <WebSocketContext.Provider value={{ connected, subscribe, unsubscribe }}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocketContext = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocketContext must be used within WebSocketProvider');
  }
  return context;
};
