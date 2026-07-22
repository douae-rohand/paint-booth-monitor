/**
 * useChatbot – send RAG queries and maintain conversation history
 */
import { useState } from 'react';
import { sendQuery, type ChatMessage, type ChatResponse } from '../api/chatbot';

export const useChatbot = () => {
  const [history, setHistory] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const ask = async (message: string): Promise<ChatResponse | null> => {
    setLoading(true);
    setError(null);
    try {
      const response = await sendQuery(message, history);
      setHistory((prev) => [
        ...prev,
        { role: 'user', content: message },
        { role: 'assistant', content: response.answer },
      ]);
      return response;
    } catch (e) {
      setError(e as Error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const clearHistory = () => setHistory([]);

  return { history, loading, error, ask, clearHistory };
};
