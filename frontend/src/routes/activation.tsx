import { createFileRoute, useNavigate, useSearch } from '@tanstack/react-router';
import { useState, useEffect } from 'react';
import { Eye, EyeOff, Lock, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { activerCompte, type ActivationCompteDTO } from '../api/admin/superviseurs';
import { isPasswordValid } from '../lib/password-rules';
import PasswordStrengthIndicator from '../components/auth/PasswordStrengthIndicator';

export const Route = createFileRoute('/activation')({
  validateSearch: (search: Record<string, unknown>) => ({
    token: typeof search.token === 'string' ? search.token : undefined,
  }),
  component: ActivationPage,
});

function ActivationPage() {
  const navigate = useNavigate();
  const search = useSearch({ from: '/activation' });
  const token = search.token as string | undefined;

  const [nouveauMotDePasse, setNouveauMotDePasse] = useState('');
  const [confirmationMotDePasse, setConfirmationMotDePasse] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [tokenValid, setTokenValid] = useState<boolean | null>(null);
  const [showIndicator, setShowIndicator] = useState(false);

  useEffect(() => {
    if (!token) {
      setTokenValid(false);
      setError('Token manquant. Veuillez utiliser le lien d\'activation complet.');
    } else {
      setTokenValid(true);
    }
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!nouveauMotDePasse || !confirmationMotDePasse) {
      setError('Veuillez remplir tous les champs.');
      return;
    }

    if (nouveauMotDePasse !== confirmationMotDePasse) {
      setError('Les mots de passe ne correspondent pas.');
      return;
    }

    setIsSubmitting(true);
    try {
      const data: ActivationCompteDTO = {
        token: token || '',
        nouveauMotDePasse,
        confirmationMotDePasse,
      };
      await activerCompte(data);
      setSuccess(true);
      // Redirect to login after 3 seconds
      setTimeout(() => {
        navigate({ to: '/login' });
      }, 3000);
    } catch (err) {
      const errorResponse = err as { response?: { data?: { message?: string } } };
      setError(
        errorResponse.response?.data?.message ||
        'Ce lien d\'activation n\'est plus valide. Contactez votre administrateur.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  if (tokenValid === false) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
        <div className="w-full max-w-md neu-card p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--danger-soft)]">
            <AlertTriangle className="h-7 w-7 text-[color:var(--danger)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Lien invalide</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            {error || 'Ce lien d\'activation n\'est plus valide. Contactez votre administrateur.'}
          </p>
          <Button
            onClick={() => navigate({ to: '/login' })}
            className="mt-6"
            style={{ boxShadow: 'var(--shadow-glow)' }}
          >
            Aller à la page de connexion
          </Button>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
        <div className="w-full max-w-md neu-card p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--success)]/15">
            <CheckCircle2 className="h-7 w-7 text-[color:var(--success)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Compte activé avec succès</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.
          </p>
          <p className="mt-4 text-xs text-muted-foreground">
            Redirection vers la page de connexion...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
      <div className="w-full max-w-md neu-card p-8 sm:p-10">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-3" style={{ color: 'var(--foreground)' }}>
            Activation du compte
          </h1>
          <p className="text-muted-foreground">
            Définissez votre mot de passe pour activer votre compte superviseur
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="nouveauMotDePasse" className="text-base">Nouveau mot de passe</Label>
            <div className="relative">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                id="nouveauMotDePasse"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={nouveauMotDePasse}
                onChange={(e) => { setNouveauMotDePasse(e.target.value); setShowIndicator(true); }}
                onFocus={() => setShowIndicator(true)}
                className="pl-14 pr-14 h-14 neu-inset border-0 focus-visible:ring-2 text-base placeholder:text-muted-foreground/70"
                required
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                disabled={isSubmitting}
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
            <PasswordStrengthIndicator password={nouveauMotDePasse} show={showIndicator} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmationMotDePasse" className="text-base">Confirmation du mot de passe</Label>
            <div className="relative">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                id="confirmationMotDePasse"
                type={showConfirmation ? 'text' : 'password'}
                placeholder="••••••••"
                value={confirmationMotDePasse}
                onChange={(e) => setConfirmationMotDePasse(e.target.value)}
                className="pl-14 pr-14 h-14 neu-inset border-0 focus-visible:ring-2 text-base placeholder:text-muted-foreground/70"
                required
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowConfirmation(!showConfirmation)}
                className="absolute right-5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                disabled={isSubmitting}
              >
                {showConfirmation ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-4 rounded-xl text-center text-sm flex items-center justify-center gap-2" style={{ 
              background: 'var(--danger-soft)',
              color: 'var(--destructive)'
            }}>
              <AlertTriangle className="w-4 h-4" />
              {error}
            </div>
          )}

          <Button
            type="submit"
            className="w-full h-12 text-base font-semibold transition-all duration-200 hover:scale-[1.01] active:scale-[0.99]"
            style={{ boxShadow: 'var(--shadow-glow)' }}
            disabled={isSubmitting || !isPasswordValid(nouveauMotDePasse)}
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Activation en cours...
              </>
            ) : (
              'Activer mon compte'
            )}
          </Button>
        </form>

        <div className="mt-6 text-center">
          <button
            type="button"
            onClick={() => navigate({ to: '/login' })}
            className="text-sm font-medium hover:opacity-80 transition-opacity"
            style={{ color: 'var(--primary)' }}
          >
            Retour à la page de connexion
          </button>
        </div>
      </div>
    </div>
  );
}
