export const createWebSocketConnection = (onMessage, onError) => {
  const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8081/ws';
  const token = localStorage.getItem('token');
  const finalUrl = token ? `${wsUrl}?token=${token}` : wsUrl;
  
  const ws = new WebSocket(finalUrl);
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onMessage(data);
    } catch (e) {
      onMessage(event.data);
    }
  };
  
  ws.onerror = (error) => {
    if (onError) onError(error);
  };
  
  return ws;
};
export default createWebSocketConnection;
