import { QueryClient } from "@tanstack/react-query";
import { createRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";
import { resolveAuthState } from './lib/auth-check';
import type { RouterContext } from './lib/types';

export const getRouter = () => {
  const queryClient = new QueryClient();

  const context: RouterContext = {
    queryClient,
    auth: {
      ensureAuthResolved: async () => {
        const { user } = await resolveAuthState();
        return { isAuthenticated: !!user, user };
      },
    },
  };

  const router = createRouter({
    routeTree,
    context,
    scrollRestoration: true,
    defaultPreloadStaleTime: 0,
  });

  return router;
};
