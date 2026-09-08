/**
 * CHATBOT API — Questions en langage naturel (tool calling, Spring AI)
 * Consumes: POST /api/chatbot/query
 */
import apiClient from '../../lib/axios';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatResponse {
  answer: string;
  sources?: string[];
}

export const sendQuery = async (message: string, history?: ChatMessage[]): Promise<ChatResponse> => {
  const response = await apiClient.post<ChatResponse>('/api/chatbot/query', { message, history });
  return response.data;
};
