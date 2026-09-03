import { BellOff, BellRing, Loader2, SmartphoneNfc, AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { UsePushNotificationsReturn } from '@/hooks/usePushNotifications';

interface PushNotificationToggleProps {
  hook: UsePushNotificationsReturn;
}

export function PushNotificationToggle({ hook }: PushNotificationToggleProps) {
  const { supporte, permission, abonneActif, chargement, erreur, activerPush, desactiverPush } = hook;

  // ── Non supporté par le navigateur ───────────────────────────────────────

  if (!supporte) {
    return (
      <div className="flex items-start gap-2.5 px-4 py-3 border-t border-border/40">
        <SmartphoneNfc className="h-4 w-4 shrink-0 mt-0.5 text-muted-foreground/50" />
        <p className="text-[11px] text-muted-foreground/70 leading-snug">
          Les notifications push ne sont pas supportées par ce navigateur.
        </p>
      </div>
    );
  }

  // ── Permission refusée par le navigateur ──────────────────────────────────

  if (permission === 'refuse') {
    return (
      <div className="flex items-start gap-2.5 px-4 py-3 border-t border-border/40">
        <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-amber-500" />
        <p className="text-[11px] text-muted-foreground leading-snug">
          Notifications push bloquées.{' '}
          <span className="text-foreground font-semibold">
            Réactivez-les dans les paramètres du navigateur.
          </span>
        </p>
      </div>
    );
  }

  // ── Activé : bouton Désactiver ────────────────────────────────────────────

  if (abonneActif) {
    return (
      <div className="flex items-center justify-between gap-3 px-4 py-3 border-t border-border/40">
        <div className="flex items-center gap-2">
          <BellRing className="h-4 w-4 shrink-0 text-emerald-500" />
          <span className="text-[11px] font-semibold text-foreground">
            Notifications push activées
          </span>
        </div>
        <button
          type="button"
          onClick={desactiverPush}
          disabled={chargement}
          className={cn(
            'flex items-center gap-1 rounded-xl px-2.5 py-1 text-[11px] font-semibold transition-all',
            'bg-muted text-muted-foreground hover:bg-destructive/10 hover:text-destructive',
            chargement && 'opacity-50 cursor-not-allowed',
          )}
        >
          {chargement ? (
            <Loader2 className="h-3 w-3 animate-spin" />
          ) : (
            <BellOff className="h-3 w-3" />
          )}
          Désactiver
        </button>

        {erreur && (
          <p className="text-[10px] text-destructive mt-1">{erreur}</p>
        )}
      </div>
    );
  }

  // ── Non activé : bouton Activer ───────────────────────────────────────────

  return (
    <div className="flex flex-col gap-1.5 px-4 py-3 border-t border-border/40">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <BellOff className="h-4 w-4 shrink-0 text-muted-foreground" />
          <span className="text-[11px] text-muted-foreground">
            Notifications push désactivées
          </span>
        </div>
        <button
          type="button"
          onClick={activerPush}
          disabled={chargement}
          className={cn(
            'flex items-center gap-1 rounded-xl px-2.5 py-1 text-[11px] font-semibold transition-all',
            'bg-primary/10 text-primary hover:bg-primary/20',
            chargement && 'opacity-50 cursor-not-allowed',
          )}
        >
          {chargement ? (
            <Loader2 className="h-3 w-3 animate-spin" />
          ) : (
            <BellRing className="h-3 w-3" />
          )}
          Activer
        </button>
      </div>

      {erreur && (
        <p className="text-[10px] text-destructive leading-snug">{erreur}</p>
      )}
    </div>
  );
}
