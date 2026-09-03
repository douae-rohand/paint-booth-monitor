-- V46 : Ajout de ACTIVATION_SUPERVISEUR dans les valeurs autorisées pour les actions d'audit
-- Distingue la réactivation d'un compte par l'Admin (ACTIVATION_SUPERVISEUR)
-- de la modification de profil (MODIFICATION_SUPERVISEUR).

ALTER TABLE log_audit
    DROP CONSTRAINT IF EXISTS log_audit_action_check;

ALTER TABLE log_audit
    ADD CONSTRAINT log_audit_action_check
        CHECK (action IN (
            'CONNEXION',
            'DECONNEXION',
            'TENTATIVE_CONNEXION_ECHOUEE',
            'CREATION_SUPERVISEUR',
            'COMPTE_ACTIVE_SUPERVISEUR',
            'ACTIVATION_SUPERVISEUR',
            'MODIFICATION_SUPERVISEUR',
            'DESACTIVATION_SUPERVISEUR',
            'EXPORT_MESURES',
            'GENERER_RAPPORT',
            'TELECHARGEMENT_RAPPORT',
            'MODIFICATION_CONFIGURATION_PLC',
            'MODIFICATION_CONFIGURATION_SEUILS'
        ));
