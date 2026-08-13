package com.projet.notifications.service;

/**
 * Contrat d'envoi d'email — indépendant du fournisseur.
 *
 * Pour changer de fournisseur (Mailgun, SES, Brevo…) : implémenter cette interface
 * et déclarer le nouveau bean. EmailWorkerService et les appelants de envoyerLienActivation
 * ne nécessitent aucune modification.
 */
public interface EmailService {

    // ── Types partagés ────────────────────────────────────────────────────────

    /**
     * Classification sémantique du résultat d'un envoi.
     *
     * SUCCES           → email accepté par le fournisseur
     * ECHEC_TEMPORAIRE → erreur transitoire, un retry a du sens (réseau, rate limit, 5xx)
     * ECHEC_DEFINITIF  → erreur permanente, un retry est inutile (adresse invalide, auth, config)
     */
    enum EmailStatus {
        SUCCES,
        ECHEC_TEMPORAIRE,
        ECHEC_DEFINITIF
    }

    /**
     * Résultat d'un appel d'envoi.
     *
     * @param statut  classification du résultat (jamais null)
     * @param erreur  message d'erreur à stocker dans derniere_erreur (null si SUCCES)
     */
    record EmailResult(EmailStatus statut, String erreur) {

        /** Raccourci pour les tests et les appelants internes. */
        public static EmailResult succes() {
            return new EmailResult(EmailStatus.SUCCES, null);
        }

        public static EmailResult echecTemporaire(String erreur) {
            return new EmailResult(EmailStatus.ECHEC_TEMPORAIRE, erreur);
        }

        public static EmailResult echecDefinitif(String erreur) {
            return new EmailResult(EmailStatus.ECHEC_DEFINITIF, erreur);
        }

        public boolean isSucces() {
            return statut == EmailStatus.SUCCES;
        }

        public boolean isEchecTemporaire() {
            return statut == EmailStatus.ECHEC_TEMPORAIRE;
        }

        public boolean isEchecDefinitif() {
            return statut == EmailStatus.ECHEC_DEFINITIF;
        }
    }

    // ── Méthodes du contrat ───────────────────────────────────────────────────

    /**
     * Envoie un email de notification (alerte, rapport, etc.).
     * Retourne un {@link EmailResult} — ne lève jamais d'exception pour le flux normal.
     *
     * @param destinataireEmail adresse du destinataire (déjà validée en amont)
     * @param titre             sujet de l'email
     * @param contenu           corps de l'email (texte brut)
     * @return résultat sémantique de l'envoi
     */
    EmailResult envoyerNotification(String destinataireEmail, String titre, String contenu);

    /**
     * Envoie le lien d'activation de compte.
     * Ne retourne pas de résultat — les erreurs sont loguées par l'implémentation.
     *
     * @param email           adresse du nouveau superviseur
     * @param lienActivation  URL d'activation à inclure dans l'email
     */
    EmailResult envoyerLienActivation(String email, String lienActivation);

    /**
     * Envoie le lien de réinitialisation de mot de passe.
     *
     * @param email                adresse de l'utilisateur
     * @param lienReinitialisation URL de réinitialisation à inclure dans l'email
     * @return résultat sémantique de l'envoi
     */
    EmailResult envoyerLienReinitialisation(String email, String lienReinitialisation);

    /**
     * Envoie l'email de notification de désactivation de compte.
     *
     * @param email  adresse de l'utilisateur
     * @param prenom prenom de l'utilisateur
     * @return résultat sémantique de l'envoi
     */
    EmailResult envoyerNotificationDesactivation(String email, String prenom);

    /**
     * Envoie l'email de bienvenue après activation réussie.
     *
     * @param email  adresse de l'utilisateur
     * @param prenom prenom de l'utilisateur
     * @return résultat sémantique de l'envoi
     */
    EmailResult envoyerEmailBienvenue(String email, String prenom);

    /**
     * Envoie un email de notification d'alerte avec un template HTML et un bouton CTA vers le tableau de bord.
     *
     * @param destinataireEmail adresse du superviseur
     * @param sujet             sujet du mail (ex: "[CRITIQUE] Anomalie détectée - TEMPERATURE")
     * @param metrique          nom de la métrique (ex: TEMPERATURE)
     * @param typeAlerte        nature de l'anomalie (ex: SEUIL_DEPASSE)
     * @param severite          niveau de sévérité (ex: CRITIQUE)
     * @param dateHeure         horodatage de l'alerte
     * @param idAlerte          identifiant de l'alerte pour traçabilité
     * @param urlTableauBord    lien vers le dashboard
     * @param emplacement       emplacement de la mesure
     * @param pointMesureNom    nom du point de mesure
     * @return résultat sémantique de l'envoi
     */
    EmailResult envoyerNotificationAlerte(
            String destinataireEmail,
            String sujet,
            String metrique,
            String typeAlerte,
            String severite,
            String dateHeure,
            String idAlerte,
            String urlTableauBord,
            String emplacement,
            String pointMesureNom
    );
}
