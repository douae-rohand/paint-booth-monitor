import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { format } from "date-fns";
import { fr } from "date-fns/locale";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Formate un nombre d'heures (nullable) en chaîne lisible.
 * null  → "N/A"
 * < 24  → "Xh"
 * ≥ 24  → "Xj Xh" (si reste > 0) ou "Xj"
 */
export function formatDureeHeures(heures: number | null | undefined): string {
  if (heures == null) return "N/A";
  const jours = Math.floor(heures / 24);
  const h = Math.round(heures % 24);
  if (jours === 0) return `${h}h`;
  if (h === 0) return `${jours}j`;
  return `${jours}j ${h}h`;
}

/**
 * Formate l'axe des abscisses du graphe selon la granularité appliquée.
 * Source de vérité : backend (response.granulariteAppliquee)
 *
 * @param horodatage - Horodatage du point de données (format ISO-8601)
 * @param granularite - Granularité appliquée (TRENTE_MIN, HORAIRE, JOURNALIERE, MENSUELLE)
 * @returns Formatted string for X-axis label
 */
export function formatAxeGraphique(horodatage: string, granularite: 'TRENTE_MIN' | 'HORAIRE' | 'JOURNALIERE' | 'MENSUELLE'): string {
  const date = new Date(horodatage);

  switch (granularite) {
    case 'TRENTE_MIN':
      // Format "HH:mm" (ex: 14:30)
      return format(date, 'HH:mm', { locale: fr });

    case 'HORAIRE':
      // Format "HH:mm" (ex: 14:00)
      // Note: le jour peut être affiché en label secondaire si la période dépasse 24h
      return format(date, 'HH:mm', { locale: fr });

    case 'JOURNALIERE':
      // Format "dd/MM" (ex: 15/07)
      return format(date, 'dd/MM', { locale: fr });

    case 'MENSUELLE':
      // Format "MMM yyyy" (ex: juil. 2026)
      return format(date, 'MMM yyyy', { locale: fr });

    default:
      return format(date, 'dd/MM HH:mm', { locale: fr });
  }
}
