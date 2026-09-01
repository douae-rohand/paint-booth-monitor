/**
 * API notifications IN_APP
 * Consomme les endpoints Java :
 *   GET    /api/notifications                  → liste combinée (non-lues + lues récentes)
 *   GET    /api/notifications/non-lues/count   → badge count
 *   PATCH  /api/notifications/{id}/lu          → marquer lue
 *   PATCH  /api/notifications/lu-tout          → tout marquer lu
 */
import apiClient from '@/lib/axios';

// ── Types ─────────────────────────────────────────────────────────────────────

export type TypeEvenement =
  | 'ALERTE_CREE'
  | 'ALERTE_RESOLU'
  | 'RAPPORT_GENERE'
  | 'COMPTE_ACTIVEE'
  | 'CONFIG_SEUILS_MODIFIE';

/** Miroir de NotificationInAppDTO.java */
export interface NotificationInAppDTO {
  idEnvoi: string;
  idNotification: string;
  typeEvenement: TypeEvenement;
  titre: string;
  contenu: string;
  lu: boolean;
  dateCreation: string;   // ISO "yyyy-MM-dd'T'HH:mm:ss"
  dateLecture: string | null;
}

// ── Appels REST ───────────────────────────────────────────────────────────────

/**
 * Liste combinée pour le panel : toutes les non-lues + lues des 7 derniers jours (max 5).
 * Retourne un tableau simple — le backend garantit toutes les non-lues sans troncature.
 */
export const getNotifications = async (): Promise<NotificationInAppDTO[]> => {
  const res = await apiClient.get<NotificationInAppDTO[]>('/api/notifications');
  return res.data;
};

/** Compteur de notifications non lues pour le badge bell. */
export const getNonLuesCount = async (): Promise<number> => {
  const res = await apiClient.get<{ count: number }>('/api/notifications/non-lues/count');
  return res.data.count;
};

/** Marque une notification comme lue. */
export const marquerLu = async (idEnvoi: string): Promise<void> => {
  await apiClient.patch(`/api/notifications/${idEnvoi}/lu`);
};

/** Marque toutes les notifications non lues comme lues. */
export const marquerToutLu = async (): Promise<void> => {
  await apiClient.patch('/api/notifications/lu-tout');
};
