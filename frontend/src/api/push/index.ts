/**
 * PUSH API — Gestion des abonnements Web Push (VAPID)
 * Endpoints backend :
 *   GET    /api/notifications/push/cle-publique  → clé VAPID publique
 *   POST   /api/notifications/push/abonnement    → créer/mettre à jour un abonnement
 *   DELETE /api/notifications/push/abonnement    → supprimer un abonnement
 */
import apiClient from '@/lib/axios';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface PushSubscriptionRequestDTO {
  endpoint: string;
  cleP256dh: string;
  cleAuth: string;
  userAgent?: string;
}

export interface PushSubscriptionResponseDTO {
  id: string;
  endpoint: string;
  dateCreation: string;
}

// ── Fonctions API ─────────────────────────────────────────────────────────────

/** Récupère la clé publique VAPID pour s'abonner via pushManager.subscribe(). */
export const getClePubliqueVapid = async (): Promise<string> => {
  const res = await apiClient.get<string>('/api/notifications/push/cle-publique');
  return res.data;
};

/** Envoie l'abonnement push du navigateur au backend. */
export const creerAbonnement = async (
  dto: PushSubscriptionRequestDTO,
): Promise<PushSubscriptionResponseDTO> => {
  const res = await apiClient.post<PushSubscriptionResponseDTO>(
    '/api/notifications/push/abonnement',
    dto,
  );
  return res.data;
};

/** Supprime l'abonnement push identifié par son endpoint. */
export const supprimerAbonnement = async (endpoint: string): Promise<void> => {
  await apiClient.delete('/api/notifications/push/abonnement', {
    params: { endpoint },
  });
};
