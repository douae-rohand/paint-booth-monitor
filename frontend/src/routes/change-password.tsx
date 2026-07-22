import React, { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Eye, EyeOff, Lock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';
import * as authApi from '@/api/auth/index';

/**
 * ChangePassword – Compact modal with blurred background
 */
const ChangePassword: React.FC = () => {
  const { mustChangePassword, updateMustChangePassword, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Si l'utilisateur n'a pas besoin de changer de mot de passe, rediriger vers le dashboard
  React.useEffect(() => {
    if (!authLoading && !mustChangePassword) {
      navigate({ to: '/dashboard' });
    }
  }, [mustChangePassword, authLoading, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validation côté client
    if (newPassword.length < 8) {
      setError('Le nouveau mot de passe doit contenir au moins 8 caractères');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Les nouveaux mots de passe ne correspondent pas');
      return;
    }

    setLoading(true);
    try {
      await authApi.changePassword({ oldPassword, newPassword });
      updateMustChangePassword(false);
      navigate({ to: '/dashboard' });
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Une erreur est survenue');
    } finally {
      setLoading(false);
    }
  };

  if (authLoading) {
    return (
      <div className="fixed inset-0 flex items-center justify-center" style={{ background: 'rgba(15, 23, 42, 0.7)', backdropFilter: 'blur(8px)' }}>
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary" />
      </div>
    );
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center" style={{ background: 'rgba(15, 23, 42, 0.7)', backdropFilter: 'blur(8px)' }}>
      <div className="w-full max-w-sm neu-card p-6 sm:p-7">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold mb-2" style={{ color: 'var(--foreground)' }}>
            Changer de mot de passe
          </h1>
          <p className="text-sm text-muted-foreground">
            Pour votre sécurité, veuillez définir un nouveau mot de passe
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Ancien mot de passe */}
          <div className="space-y-3">
            <Label htmlFor="oldPassword" className="text-sm font-medium">Ancien mot de passe</Label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="oldPassword"
                type={showOldPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                className={cn(
                  "pl-11 pr-11 h-11 neu-inset border-0 focus-visible:ring-2 text-sm placeholder:text-muted-foreground/70",
                  error && "ring-2 ring-destructive"
                )}
                required
              />
              <button
                type="button"
                onClick={() => setShowOldPassword(!showOldPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              >
                {showOldPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Nouveau mot de passe */}
          <div className="space-y-3">
            <Label htmlFor="newPassword" className="text-sm font-medium">Nouveau mot de passe</Label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="newPassword"
                type={showNewPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className={cn(
                  "pl-11 pr-11 h-11 neu-inset border-0 focus-visible:ring-2 text-sm placeholder:text-muted-foreground/70",
                  error && "ring-2 ring-destructive"
                )}
                required
              />
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              >
                {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Confirmer nouveau mot de passe */}
          <div className="space-y-3">
            <Label htmlFor="confirmPassword" className="text-sm font-medium">Confirmer le nouveau mot de passe</Label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={cn(
                  "pl-11 pr-11 h-11 neu-inset border-0 focus-visible:ring-2 text-sm placeholder:text-muted-foreground/70",
                  error && "ring-2 ring-destructive"
                )}
                required
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-3 rounded-xl text-center text-xs" style={{ 
              background: 'var(--danger-soft)',
              color: 'var(--destructive)'
            }}>
              {error}
            </div>
          )}

          <Button
            type="submit"
            className="w-full h-10 text-sm font-semibold transition-all duration-200 hover:scale-[1.01] active:scale-[0.99]"
            style={{ boxShadow: 'var(--shadow-glow)' }}
            disabled={loading}
          >
            {loading ? 'Mise à jour en cours...' : 'Mettre à jour le mot de passe'}
          </Button>
        </form>
      </div>
    </div>
  );
};

// Route definition
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/change-password')({
  component: ChangePassword,
});
