import { createFileRoute, useNavigate, useSearch } from '@tanstack/react-router';
import { useState, useEffect } from 'react';
import { Eye, EyeOff, Lock, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { reinitialiserMotDePasse } from '../api/auth/mot-de-passe-oublie';
import { isPasswordValid } from '../lib/password-rules';
import PasswordStrengthIndicator from '../components/auth/PasswordStrengthIndicator';

export const Route = createFileRoute('/reinitialiser-mot-de-passe')({
  validateSearch: (search: Record<string, unknown>) => ({
    token: typeof search.token === 'string' ? search.token : undefined,
  }),
  component: ReinitialiserMotDePassePage,
});

function ReinitialiserMotDePassePage() {
  const navigate = useNavigate();
  const search = useSearch({ from: '/reinitialiser-mot-de-passe' });
  const token = search.token as string | undefined;

  const [nouveauMotDePasse, setNouveauMotDePasse] = useState('');
  const [confirmationMotDePasse, setConfirmationMotDePasse] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [tokenValid, setTokenValid] = useState<boolean | null>(null);
  const [countdown, setCountdown] = useState(5);
  const [showIndicator, setShowIndicator] = useState(false);

  // Validation immédiate de la présence du token dans l'URL
  useEffect(() => {
    if (!token || token.trim() === '') {
      setTokenValid(false);
    } else {
      setTokenValid(true);
    }
  }, [token]);

  // Countdown et redirection automatique après succès
  useEffect(() => {
    if (!success) return;
    if (countdown <= 0) {
      navigate({ to: '/login' });
      return;
    }
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [success, countdown, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validations côté client — mêmes règles que activation.tsx
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
      await reinitialiserMotDePasse({
        token: token!,
        nouveauMotDePasse,
        confirmationMotDePasse,
      });
      setSuccess(true);
    } catch (err) {
      const errorResponse = err as { response?: { data?: { message?: string }; status?: number } };
      const status = errorResponse.response?.status;
      const serverMessage = errorResponse.response?.data?.message;

      // Messages explicites selon le cas d'erreur renvoyé par le backend
      if (status === 400) {
        if (serverMessage?.toLowerCase().includes('expir')) {
          setError('Ce lien de réinitialisation a expiré (validité 15 minutes). Faites une nouvelle demande.');
        } else if (serverMessage?.toLowerCase().includes('utilis') || serverMessage?.toLowerCase().includes('invalid')) {
          setError('Ce lien de réinitialisation a déjà été utilisé ou est invalide. Faites une nouvelle demande.');
        } else {
          setError(serverMessage || 'Le lien de réinitialisation est invalide ou expiré.');
        }
      } else {
        setError('Une erreur est survenue. Veuillez réessayer dans quelques instants.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── État : token absent de l'URL ──────────────────────────────────────────
  if (tokenValid === false) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
        <div className="w-full max-w-md neu-card p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--danger-soft)]">
            <AlertTriangle className="h-7 w-7 text-[color:var(--danger)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Lien invalide</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Ce lien de réinitialisation est invalide ou incomplet.
            Veuillez refaire une demande de réinitialisation.
          </p>
          <Button
            onClick={() => navigate({ to: '/login' })}
            className="mt-6"
            style={{ boxShadow: 'var(--shadow-glow)' }}
          >
            Retour à la connexion
          </Button>
        </div>
      </div>
    );
  }

  // ── État : succès ──────────────────────────────────────────────────────────
  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
        <div className="w-full max-w-md neu-card p-10 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--success)]/15">
            <CheckCircle2 className="h-7 w-7 text-[color:var(--success)]" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Mot de passe réinitialisé</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Votre mot de passe a été mis à jour avec succès.
          </p>
          <p className="mt-4 text-xs text-muted-foreground">
            Redirection dans <span className="font-semibold text-foreground">{countdown}</span> seconde{countdown > 1 ? 's' : ''}...
          </p>
          <Button
            onClick={() => navigate({ to: '/login' })}
            className="mt-6 w-full"
            style={{ boxShadow: 'var(--shadow-glow)' }}
          >
            Se connecter maintenant
          </Button>
        </div>
      </div>
    );
  }

  // ── État : chargement initial (token pas encore vérifié) ──────────────────
  if (tokenValid === null) return null;

  // ── État : formulaire ──────────────────────────────────────────────────────
  return (
    <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}>
      <div className="w-full max-w-md neu-card p-8 sm:p-10">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-3" style={{ color: 'var(--foreground)' }}>
            Nouveau mot de passe
          </h1>
          <p className="text-muted-foreground">
            Choisissez un mot de passe sécurisé pour votre compte
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Nouveau mot de passe */}
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
                autoComplete="new-password"
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

          {/* Confirmation */}
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
                autoComplete="new-password"
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

          {/* Message d'erreur */}
          {error && (
            <div
              className="p-4 rounded-xl text-center text-sm flex items-center justify-center gap-2"
              style={{ background: 'var(--danger-soft)', color: 'var(--destructive)' }}
            >
              <AlertTriangle className="w-4 h-4 shrink-0" />
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
                Réinitialisation en cours...
              </>
            ) : (
              'Réinitialiser le mot de passe'
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
