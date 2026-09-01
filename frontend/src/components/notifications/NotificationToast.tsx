import { useEffect } from 'react';
import { X, BellRing } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { NotificationInAppDTO, TypeEvenement } from '@/api/notifications';

const TOAST_DURATION_MS = 5000;

interface NotificationToastProps {
  notification: NotificationInAppDTO | null;
  onDismiss: () => void;
}

function bellIconColor(type: TypeEvenement): string {
  switch (type) {
    case 'ALERTE_CREE':           return 'text-rose-500';
    case 'ALERTE_RESOLU':         return 'text-emerald-500';
    case 'CONFIG_SEUILS_MODIFIE': return 'text-amber-500';
    case 'COMPTE_ACTIVEE':        return 'text-blue-500';
    case 'RAPPORT_GENERE':        return 'text-violet-500';
    default:                      return 'text-primary';
  }
}

export function NotificationToast({ notification, onDismiss }: NotificationToastProps) {
  useEffect(() => {
    if (!notification) return;
    const timer = setTimeout(onDismiss, TOAST_DURATION_MS);
    return () => clearTimeout(timer);
  }, [notification, onDismiss]);

  if (!notification) return null;

  return (
    <div
      className="fixed top-20 right-6 z-[9999] w-80 rounded-2xl border border-border/60 bg-background shadow-xl animate-in slide-in-from-top-4 fade-in duration-300"
      role="alert"
      aria-live="polite"
    >
      <div className="flex items-start gap-3 p-4">
        {/* Icône colorée selon le type */}
        <div className="shrink-0 mt-0.5">
          <BellRing className={cn("h-4 w-4 animate-pulse", bellIconColor(notification.typeEvenement))} />
        </div>

        {/* Contenu */}
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold text-foreground leading-snug">
            {notification.titre}
          </p>
          <p className="mt-1 text-[11px] text-muted-foreground line-clamp-2">
            {notification.contenu}
          </p>
        </div>

        {/* Bouton fermer */}
        <button
          type="button"
          onClick={onDismiss}
          className="shrink-0 -mt-0.5 -mr-1 flex h-6 w-6 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
          aria-label="Fermer la notification"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>

      {/* Barre de progression */}
      <div className="px-4 pb-3">
        <div className="h-0.5 w-full rounded-full bg-muted overflow-hidden">
          <div
            className="h-full bg-primary/40 rounded-full"
            style={{ animation: `shrink ${TOAST_DURATION_MS}ms linear forwards` }}
          />
        </div>
      </div>

      <style>{`
        @keyframes shrink {
          from { width: 100%; }
          to   { width: 0%; }
        }
      `}</style>
    </div>
  );
}
