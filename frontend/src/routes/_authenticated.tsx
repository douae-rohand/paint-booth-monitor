import { createFileRoute, redirect, Outlet } from '@tanstack/react-router';
import type { RouterContext } from '../lib/types';

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async (opts) => {
    const { isAuthenticated, user } = await (opts.context as RouterContext).auth.ensureAuthResolved();
    if (!isAuthenticated) {
      throw redirect({ to: '/login' } as any);
    }
    if (user?.mustChangePassword) {
      throw redirect({ to: '/change-password' } as any);
    }
  },
  component: () => <Outlet />,
});
