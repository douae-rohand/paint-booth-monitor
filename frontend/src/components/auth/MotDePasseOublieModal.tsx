import React, { useState, useEffect, useRef } from 'react';
import { Mail, X, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { demanderReinitialisation } from '../../api/auth/mot-de-passe-oublie';

interface MotDePasseOublieModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type ModalState = 'form' | 'success' | 'error';

const MotDePasseOublieModal: React.FC<MotDePasseOublieModalProps> = ({ isOpen, onClose }) => {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalState, setModalState] = useState<ModalState>('form');
  const [networkError, setNetworkError] = useState<string | null>(null);
  const emailInputRef = useRef<HTMLInputElement>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Focus email input when modal opens
  useEffect(() => {
    if (isOpen && modalState === 'form') {
      setTimeout(() => emailInputRef.current?.focus(), 50);
    }
  }, [isOpen, modalState]);

  // Auto-close after success
  useEffect(() => {
    if (modalState === 'success') {
      closeTimerRef.current = setTimeout(() => {
        handleClose();
      }, 5000);
    }
    return () => {
      if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
    };
  }, [modalState]);

  // Close on Escape key
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) handleClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [isOpen, isSubmitting]);

  const handleClose = () => {
    if (isSubmitting) return;
    setEmail('');
    setModalState('form');
    setNetworkError(null);
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setNetworkError(null);
    setIsSubmitting(true);

    try {
      await demanderReinitialisation({ email });
      // Toujours afficher le message générique, même si l'email n'existe pas
      setModalState('success');
    } catch (err) {
      // Uniquement les erreurs réseau/serveur (5xx, timeout) — jamais "email non trouvé"
      const error = err as { response?: { status?: number } };
      const status = error.response?.status;
      if (!status || status >= 500) {
        setNetworkError(
          'Une erreur est survenue. Veuillez réessayer dans quelques instants.',
        );
      } else {
        // Pour tout autre cas (4xx inattendu), afficher le message générique de succès
        setModalState('success');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4"
        style={{ background: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(4px)' }}
        onClick={(e) => { if (e.target === e.currentTarget) handleClose(); }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        {/* Panel */}
        <div
          className="relative w-full max-w-md neu-card p-8 animate-in fade-in zoom-in-95 duration-200 shadow-none border border-border/40"
          style={{ boxShadow: 'none' }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Close button */}
          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            className="absolute right-4 top-4 text-muted-foreground hover:text-foreground transition-colors disabled:opacity-40"
            aria-label="Fermer"
          >
            <X className="w-5 h-5" />
          </button>

          {/* ── ÉTAT : succès ── */}
          {modalState === 'success' && (
            <div className="text-center py-4">
              <div
                className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl"
                style={{ background: 'var(--success, #22c55e)15' }}
              >
                <CheckCircle2 className="h-7 w-7" style={{ color: 'var(--success, #22c55e)' }} />
              </div>
              <h2 id="modal-title" className="text-xl font-bold text-foreground mb-2">
                Email envoyé
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed">
                Si ce compte existe, un email contenant un lien de réinitialisation a été envoyé.
                Pensez à vérifier vos spams.
              </p>
              <p className="mt-3 text-xs text-muted-foreground">
                Ce lien est valable <span className="font-medium">15 minutes</span>.
              </p>
              <Button
                onClick={handleClose}
                className="mt-6 w-full"
              >
                Fermer
              </Button>
            </div>
          )}

          {/* ── ÉTAT : formulaire ── */}
          {modalState === 'form' && (
            <>
              <div className="text-center mb-6">
                <div
                  className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl"
                  style={{ background: 'color-mix(in srgb, var(--primary) 12%, transparent)' }}
                >
                  <Mail className="h-6 w-6" style={{ color: 'var(--primary)' }} />
                </div>
                <h2 id="modal-title" className="text-xl font-bold text-foreground">
                  Mot de passe oublié
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Saisissez votre adresse email pour recevoir un lien de réinitialisation.
                </p>
              </div>

              <form onSubmit={handleSubmit} className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="reset-email" className="text-base">
                    Email
                  </Label>
                  <div className="relative">
                    <Mail className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      ref={emailInputRef}
                      id="reset-email"
                      type="email"
                      placeholder="votre@email.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-14 pr-5 h-14 neu-inset border-0 focus-visible:ring-2 text-base placeholder:text-muted-foreground/70"
                      required
                      disabled={isSubmitting}
                      autoComplete="email"
                    />
                  </div>
                </div>

                {/* Erreur réseau uniquement */}
                {networkError && (
                  <div
                    className="p-4 rounded-xl text-center text-sm flex items-center justify-center gap-2"
                    style={{ background: 'var(--danger-soft)', color: 'var(--destructive)' }}
                  >
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    {networkError}
                  </div>
                )}

                <div className="flex gap-3 pt-1">
                  <Button
                    type="button"
                    variant="outline"
                    className="flex-1 h-12"
                    onClick={handleClose}
                    disabled={isSubmitting}
                  >
                    Annuler
                  </Button>
                  <Button
                    type="submit"
                    className="flex-1 h-12 font-semibold transition-all duration-200 hover:scale-[1.01] active:scale-[0.99]"
                    disabled={isSubmitting || !email}
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        Envoi en cours...
                      </>
                    ) : (
                      'Envoyer le lien'
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default MotDePasseOublieModal;
