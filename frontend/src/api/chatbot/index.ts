/**
 * CHATBOT API — Questions en langage naturel (tool calling, Spring AI)
 * Consumes: POST /api/chatbot/query
 */
import apiClient from '../../lib/axios';

export interface ChatMessage {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  isError?: boolean;
  /** true = erreur temporaire (timeout, rate-limit, réseau) → afficher bouton Retry */
  isRetryable?: boolean;
  /** question originale de l'utilisateur, nécessaire pour le retry */
  originalMessage?: string;
}

export interface ChatResponse {
  reponse: string;
}

/**
 * Envoie une question au chatbot backend.
 * Le backend reconstruit lui-même l'historique depuis la base de données.
 */
export const sendQuery = async (message: string): Promise<ChatResponse> => {
  const response = await apiClient.post<ChatResponse>('/api/chatbot/query', { message });
  return response.data;
};
