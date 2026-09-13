 /**
 * Extrait le message lisible depuis une erreur axios (format ApiErrorResponse).
 *
 * Lit `response.data.message` retourné par le backend (champ distinct du code
 * technique `response.data.code`). Retourne `fallback` si le message est absent.
 *
 * @param err      Erreur inconnue, typiquement levée par axios
 * @param fallback Message générique affiché si `response.data.message` est absent
 */
export function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const res = (err as { response?: { data?: { message?: string } } }).response;
    if (res?.data?.message) return res.data.message;
  }
  return fallback;
}

/**
 * Détermine si l'erreur est transitoire et que l'utilisateur peut relancer
 * la requête (timeout, rate-limit, réseau, service indisponible).
 * Les erreurs 400/500 liées à la logique métier ne sont pas retryable.
 */
export function isRetryableError(err: unknown): boolean {
  if (!err || typeof err !== 'object') {
    // Erreur réseau pure (pas de réponse HTTP) → retryable
    return true;
  }
  if ('response' in err) {
    const status = (err as { response?: { status?: number } }).response?.status;
    // 429 Rate Limit, 503 Service Unavailable, 504 Gateway Timeout
    return status === 429 || status === 503 || status === 504;
  }
  // Pas de propriété response (coupure réseau, CORS...) → retryable
  return true;
}
