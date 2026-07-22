type WebSocketMessageHandler = (data: unknown) => void;
type WebSocketErrorHandler = (error: Event) => void;

export const createWebSocketConnection = (
  onMessage: WebSocketMessageHandler,
  onError?: WebSocketErrorHandler
): WebSocket => {
  const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8081/ws';

  // The backend sets the JWT in an HttpOnly cookie. For security and to avoid
  // exposing tokens to JavaScript, do not read token from localStorage. Rely on
  // the browser to send cookies automatically on the WebSocket upgrade request
  // when the WS host matches the API origin and the server reads the cookie.
  const ws = new WebSocket(wsUrl);
  
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
