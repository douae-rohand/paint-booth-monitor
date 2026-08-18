package com.projet.notifications.service;

import java.util.Map;

/**
 * Contrat d'envoi d'email — indépendant du fournisseur.
 *
 * Pour changer de fournisseur (Mailgun, SES, Brevo…) : implémenter cette interface
 * et déclarer le nouveau bean. Les appelants ne nécessitent aucune modification.
 *
 * Toutes les méthodes retournent {@link EmailResult} et ne lèvent jamais d'exception
 * pour le flux normal — les erreurs sont encapsulées dans EmailResult.statut().
 */
public interface EmailService {

    // ── Types partagés ────────────────────────────────────────────────────────

    enum EmailStatus {
        SUCCES,
        ECHEC_TEMPORAIRE,
        ECHEC_DEFINITIF
    }

    record EmailResult(EmailStatus statut, String erreur) {

        public static EmailResult succes() {
            return new EmailResult(EmailStatus.SUCCES, null);
        }

        public static EmailResult echecTemporaire(String erreur) {
            return new EmailResult(EmailStatus.ECHEC_TEMPORAIRE, erreur);
        }

        public static EmailResult echecDefinitif(String erreur) {
            return new EmailResult(EmailStatus.ECHEC_DEFINITIF, erreur);
        }

        public boolean isSucces()          { return statut == EmailStatus.SUCCES; }
        public boolean isEchecTemporaire() { return statut == EmailStatus.ECHEC_TEMPORAIRE; }
        public boolean isEchecDefinitif()  { return statut == EmailStatus.ECHEC_DEFINITIF; }
    }

    // ── Emails transactionnels (hors outbox) ──────────────────────────────────

    /** Lien d'activation du compte (24h). */
    EmailResult envoyerLienActivation(String email, String lienActivation);

    /** Lien de réinitialisation du mot de passe (15min). */
    EmailResult envoyerLienReinitialisation(String email, String lienReinitialisation);

    /** Notification de désactivation de compte (envoyé au superviseur). */
    EmailResult envoyerNotificationDesactivation(String email, String prenom);

    /** Email de bienvenue après activation réussie (envoyé au superviseur). */
    EmailResult envoyerEmailBienvenue(String email, String prenom);

    // ── Emails outbox (EmailWorkerService) ────────────────────────────────────

    /**
     * Notification d'alerte avec template HTML riche (ALERTE_CREE / ALERTE_RESOLU).
     * Le titre est lu depuis Notification.titre et transmis tel quel — jamais reconstruit.
     *
     * @param donneesEvenement map JSONB contenant : idAlerte, metrique, typeAlerte, severite,
     *                         nomPointMesure, typeEmplacement, dateEvenement
     */
    EmailResult envoyerNotificationAlerte(
            String destinataireEmail,
            String titre,
            Map<String, Object> donneesEvenement
    );

    /**
     * Notification d'activation de compte (COMPTE_ACTIVEE) — envoyée aux Admins.
     * Template HTML avec tableau nom/prénom/date + bouton "Voir les superviseurs".
     * Le titre est lu depuis Notification.titre et transmis tel quel.
     *
     * @param donneesEvenement map JSONB contenant : prenomSuperviseur, nomSuperviseur, dateActivation
     */
    EmailResult envoyerNotificationCompteActive(
            String destinataireEmail,
            String titre,
            Map<String, Object> donneesEvenement
    );

    /**
     * Notification de modification de seuil (CONFIG_SEUILS_MODIFIE) — envoyée aux Superviseurs.
     * Template HTML avec tableau point/métrique/type + bouton "Voir les seuils".
     * Le titre est lu depuis Notification.titre et transmis tel quel.
     *
     * @param donneesEvenement map JSONB contenant : nomPointMesure, metrique, typeModification, dateModification
     */
    EmailResult envoyerNotificationSeuilModifie(
            String destinataireEmail,
            String titre,
            Map<String, Object> donneesEvenement
    );

    /**
     * Notification de rapport disponible (RAPPORT_GENERE) — envoyée aux Superviseurs.
     * Template HTML avec référence rapport + bouton "Télécharger" si urlRapport présent.
     * Le titre est lu depuis Notification.titre et transmis tel quel.
     *
     * @param donneesEvenement map JSONB contenant : idRapport, dateGeneration, urlRapport (optionnel)
     */
    EmailResult envoyerNotificationRapportGenere(
            String destinataireEmail,
            String titre,
            Map<String, Object> donneesEvenement
    );

    /**
     * Fallback texte brut — utilisé uniquement si donnees_evenement est null
     * (cas exceptionnel : alerte supprimée ON DELETE SET NULL, ou données manquantes).
     * Ne pas utiliser pour les flux normaux.
     */
    EmailResult envoyerNotification(String destinataireEmail, String titre, String contenu);
}
