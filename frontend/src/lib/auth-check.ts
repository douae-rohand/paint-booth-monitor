import * as authApi from '../api/auth/index';
import type { AuthUser } from '../api/auth/index';

type AuthResult = { user: AuthUser | null };

let authPromise: Promise<AuthResult> | null = null;
let cached: AuthResult | undefined = undefined;

/**
 * Resolve the current auth state once and cache the result for the session.
 * Concurrent callers receive the same promise/result.
 * On error, resolves to { user: null } (do not throw) so callers can decide.
 */
export function resolveAuthState(): Promise<AuthResult> {
  if (cached !== undefined) {
    return Promise.resolve(cached);
  }
  if (authPromise) return authPromise;

  authPromise = (async () => {
    try {
      const user = await authApi.getCurrentUser();
      const result: AuthResult = { user: user ?? null };
      cached = result;
      return result;
    } catch (e) {
      // If request fails (401 or network), treat as unauthenticated
      const result: AuthResult = { user: null };
      cached = result;
      return result;
    } finally {
      // keep authPromise so concurrent callers get same resolved value
    }
  })();

  return authPromise;
}

/**
 * Invalidate cached auth state so next resolveAuthState() will perform a network call.
 */
export function invalidateAuthCache(): void {
  cached = undefined;
  authPromise = null;
}

export default { resolveAuthState, invalidateAuthCache };
