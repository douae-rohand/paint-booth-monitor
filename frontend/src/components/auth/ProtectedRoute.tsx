import { Navigate, Outlet } from '@tanstack/react-router';
import { useAuth } from '@/contexts/AuthContext';

interface ProtectedRouteProps {
  requiredRole?: string;
}

/**
 * Route wrapper that requires authentication.
 * If requiredRole is provided (e.g. 'ROLE_ADMIN'), also checks the user's role.
 */
export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, loading, user } = useAuth();

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to={"/login" as any} />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-2 text-center">
        <h1 className="text-2xl font-bold text-destructive">403 — Accès refusé</h1>
        <p className="text-muted-foreground">Vous n&apos;avez pas les droits nécessaires pour accéder à cette page.</p>
      </div>
    );
  }

  return <Outlet />;
}

export default ProtectedRoute;
