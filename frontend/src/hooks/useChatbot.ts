/**
 * useChatbot – envoie des questions au chatbot et maintient l'historique de conversation local.
 * L'endpoint cible est POST /api/chatbot/query (module Java, Spring AI, tool calling).
 */
import { useState } from 'react';
import { sendQuery, type ChatMessage } from '../api/chatbot';
import { extractErrorMessage, isRetryableError } from '../lib/errors';

export const useChatbot = () => {
  const [history, setHistory] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Appelle le backend et ajoute la réponse (ou l'erreur) dans l'historique.
   * N'ajoute PAS de bulle utilisateur — c'est la responsabilité de l'appelant.
   */
  const _callBackend = async (trimmed: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await sendQuery(trimmed);
      const assistantMsg: ChatMessage = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response.reponse,
      };
      setHistory((prev) => [...prev, assistantMsg]);
    } catch (e) {
      const errMsg = extractErrorMessage(e, 'Une erreur est survenue lors de la communication avec CoatSense.');
      const retryable = isRetryableError(e);
      setError(errMsg);
      const errorChatMsg: ChatMessage = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: errMsg,
        isError: true,
        isRetryable: retryable,
        originalMessage: trimmed,
      };
      setHistory((prev) => [...prev, errorChatMsg]);
    } finally {
      setLoading(false);
    }
  };

  /** Envoi normal : ajoute la bulle utilisateur puis appelle le backend. */
  const ask = async (message: string) => {
    const trimmed = message.trim();
    if (!trimmed || loading) return;
    setHistory((prev) => [
      ...prev,
      { id: `user-${Date.now()}`, role: 'user', content: trimmed },
    ]);
    await _callBackend(trimmed);
  };

  /**
   * Retry : supprime uniquement la bulle d'erreur (la bulle utilisateur
   * originale reste visible) puis rappelle le backend — sans doublon.
   */
  const retry = async (originalMessage: string) => {
    setHistory((prev) =>
      prev.filter((m) => !m.isError || m.originalMessage !== originalMessage)
    );
    await _callBackend(originalMessage);
  };

  const clearHistory = () => {
    setHistory([]);
    setError(null);
  };

  return { history, loading, error, ask, retry, clearHistory };
};
