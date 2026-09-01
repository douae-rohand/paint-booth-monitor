import { useState, useRef, useEffect } from 'react';
import { Bell, CheckCheck, Loader2 } from 'lucide-react';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import { cn } from '@/lib/utils';
import type { UseNotificationsReturn } from '@/hooks/useNotifications';
import type { TypeEvenement } from '@/api/notifications';

const BELL_COLOR_DURATION_MS = 5000;

interface BellNotificationsProps {
  hook: UseNotificationsReturn;
}

function bellColor(type: TypeEvenement): string {
  switch (type) {
    case 'ALERTE_CREE':           return 'text-rose-500';
    case 'ALERTE_RESOLU':         return 'text-emerald-500';
    case 'CONFIG_SEUILS_MODIFIE': return 'text-amber-500';
    case 'COMPTE_ACTIVEE':        return 'text-blue-500';
    case 'RAPPORT_GENERE':        return 'text-violet-500';
    default:                      return 'text-primary';
  }
}

function dotColor(type: TypeEvenement): string {
  switch (type) {
    case 'ALERTE_CREE':           return 'bg-rose-500';
    case 'ALERTE_RESOLU':         return 'bg-emerald-500';
    case 'CONFIG_SEUILS_MODIFIE': return 'bg-amber-500';
    case 'COMPTE_ACTIVEE':        return 'bg-blue-500';
    case 'RAPPORT_GENERE':        return 'bg-violet-500';
    default:                      return 'bg-muted-foreground';
  }
}

function formatDate(dateStr: string): string {
  try {
    return format(new Date(dateStr), "d MMM, HH:mm", { locale: fr });
  } catch {
    return '';
  }
}

/** Plafonne le badge d'affichage à 9+. Ne modifie pas nonLuesCount. */
function formatBadgeCount(count: number): string {
  return count <= 9 ? String(count) : '9+';
}

export function BellNotifications({ hook }: BellNotificationsProps) {
  const {
    notifications,
    nonLuesCount,
    loading,
    dernierePush,
    marquerLu,
    marquerToutLu,
  } = hook;

  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [activeBellType, setActiveBellType] = useState<TypeEvenement | null>(null);

  useEffect(() => {
    if (!dernierePush) return;
    setActiveBellType(dernierePush.typeEvenement);
    const timer = setTimeout(() => setActiveBellType(null), BELL_COLOR_DURATION_MS);
    return () => clearTimeout(timer);
  }, [dernierePush]);

  useEffect(() => {
    if (!open) return;
    const handleClick = (e: MouseEvent) => {
      if (
        panelRef.current && !panelRef.current.contains(e.target as Node) &&
        buttonRef.current && !buttonRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [open]);

  const handleNotifClick = async (idEnvoi: string, lu: boolean) => {
    if (!lu) await marquerLu(idEnvoi);
  };

  const bellIconClass = activeBellType
    ? cn(bellColor(activeBellType), 'animate-bounce')
    : undefined;

  return (
    <div className="relative">
      {/* Bouton Bell */}
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((v) => !v)}
        title="Notifications"
        className={cn(
          "flex h-9 w-9 items-center justify-center rounded-xl transition-all relative",
          open
            ? "bg-primary text-primary-foreground shadow-[inset_2px_2px_5px_rgba(0,0,0,0.15)]"
            : "text-muted-foreground hover:text-foreground hover:bg-secondary"
        )}
      >
        <Bell className={cn("h-4 w-4 transition-colors duration-300", bellIconClass)} />
        {nonLuesCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive text-[9px] font-bold text-destructive-foreground px-1 border-2 border-white dark:border-background animate-in zoom-in duration-200">
            {formatBadgeCount(nonLuesCount)}
          </span>
        )}
      </button>

      {/* Panel déroulant */}
      {open && (
        <div
          ref={panelRef}
          className="absolute right-0 top-full mt-2 z-50 w-80 rounded-2xl border border-border/60 bg-background shadow-xl animate-in fade-in slide-in-from-top-2 duration-200"
        >
          {/* En-tête */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-border/50">
            <span className="text-sm font-bold">Notifications</span>
            {nonLuesCount > 0 && (
              <button
                type="button"
                onClick={() => marquerToutLu()}
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary transition-colors"
                title="Tout marquer comme lu"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Tout lire
              </button>
            )}
          </div>

          {/* Liste */}
          <div className="max-h-[420px] overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-10">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center px-4">
                <Bell className="h-8 w-8 text-muted-foreground/30 mb-2" />
                <p className="text-sm text-muted-foreground">Aucune notification</p>
              </div>
            ) : (
              <ul className="divide-y divide-border/40">
                {notifications.map((n) => (
                  <li
                    key={n.idEnvoi}
                    onClick={() => handleNotifClick(n.idEnvoi, n.lu)}
                    className="flex gap-3 px-4 py-3 cursor-pointer transition-colors hover:bg-muted/40"
                  >
                    {/* Point coloré gauche — type d'événement */}
                    <div className="mt-1.5 shrink-0">
                      <span className={cn("block h-2 w-2 rounded-full", dotColor(n.typeEvenement))} />
                    </div>

                    {/* Contenu */}
                    <div className="min-w-0 flex-1">
                      <p className="text-xs leading-snug text-foreground">
                        {n.titre}
                      </p>
                      <p className="mt-0.5 text-[11px] text-muted-foreground line-clamp-2">
                        {n.contenu}
                      </p>
                      <p className="mt-1 text-[10px] text-muted-foreground/60">
                        {formatDate(n.dateCreation)}
                      </p>
                    </div>

                    {/* Point bleu droite — indicateur non-lu */}
                    {!n.lu && (
                      <div className="shrink-0 mt-1.5">
                        <span className="block h-1.5 w-1.5 rounded-full bg-primary" />
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
