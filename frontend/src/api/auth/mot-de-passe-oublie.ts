/**
 * MOT DE PASSE OUBLIÉ – demande de réinitialisation & définition du nouveau mot de passe
 * Consumes:
 *   POST /api/auth/mot-de-passe-oublie
 *   POST /api/auth/reinitialiser-mot-de-passe
 */
import apiClient from '../../lib/axios';

export interface DemandeReinitialisationPayload {
  email: string;
}

export interface ReinitialisationMotDePassePayload {
  token: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}

/**
 * Envoie une demande de réinitialisation de mot de passe.
 * Le backend retourne toujours 200 quel que soit l'existence du compte.
 */
export const demanderReinitialisation = async (
  payload: DemandeReinitialisationPayload,
): Promise<void> => {
  await apiClient.post('/api/auth/mot-de-passe-oublie', payload);
};

/**
 * Définit le nouveau mot de passe à l'aide du token reçu par email.
 */
export const reinitialiserMotDePasse = async (
  payload: ReinitialisationMotDePassePayload,
): Promise<void> => {
  await apiClient.post('/api/auth/reinitialiser-mot-de-passe', payload);
};
