-- V45 : Retrait de VALIDATION_ANOMALIE des valeurs autorisées pour les actions d'audit dans log_audit

DELETE FROM log_audit WHERE action = 'VALIDATION_ANOMALIE';

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
            'MODIFICATION_SUPERVISEUR',
            'DESACTIVATION_SUPERVISEUR',
            'EXPORT_MESURES',
            'GENERER_RAPPORT',
            'TELECHARGEMENT_RAPPORT',
            'MODIFICATION_CONFIGURATION_PLC',
            'MODIFICATION_CONFIGURATION_SEUILS'
        ));
