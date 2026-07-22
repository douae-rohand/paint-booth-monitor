import type { QueryClient } from '@tanstack/react-query';
import type { AuthUser } from '../api/auth/index';

export type AuthHelper = {
  ensureAuthResolved: () => Promise<{ isAuthenticated: boolean; user: AuthUser | null }>;
};

export type RouterContext = {
  queryClient: QueryClient;
  auth: AuthHelper;
};

export default RouterContext;
