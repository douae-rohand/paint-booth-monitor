import { createFileRoute, isRedirect, redirect } from '@tanstack/react-router';
import LoginForm from '../components/auth/LoginForm';
import type { RouterContext } from '../lib/types';

/**
 * NOTE: beforeLoad now uses the router context `auth.ensureAuthResolved()` to avoid
 * duplicate network calls. The shared singleton in `lib/auth-check.ts` guarantees a
 * single simultaneous fetch for `/api/auth/me`.
 */

/**
 * /login route – redirects to / if already authenticated (session cookie valid).
 */
export const Route = createFileRoute('/login')({
  beforeLoad: async (opts) => {
    try {
      const { isAuthenticated } = await (opts.context as RouterContext).auth.ensureAuthResolved();
      if (isAuthenticated) {
        throw redirect({ to: '/dashboard' } as any);
      }
    } catch (error) {
      if (isRedirect(error)) {
        throw error;
      }
      // Non authentifié : rester sur la page login
    }
  },
  component: LoginPage,
});

function LoginPage() {
  return <LoginForm />;
}
