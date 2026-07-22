import { createFileRoute, redirect, Outlet } from '@tanstack/react-router';
import type { RouterContext } from '../lib/types';

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async (opts) => {
    const { isAuthenticated } = await (opts.context as RouterContext).auth.ensureAuthResolved();
    if (!isAuthenticated) {
      throw redirect({ to: '/login' } as any);
    }
  },
  component: () => <Outlet />,
});
