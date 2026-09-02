-- V44 : Mise à jour des valeurs autorisées pour les actions d'audit dans log_audit
--
-- Contexte : Alignement des valeurs de l'enum ActionAudit avec la nouvelle liste d'actions d'audit.
-- Modifications :
-- - Renommage des anciennes valeurs en BDD pour conserver la cohérence historique.
-- - Mises à jour de la contrainte CHECK log_audit_action_check.

-- ── 1. Migration des valeurs existantes ──

UPDATE log_audit SET action = 'CREATION_SUPERVISEUR' WHERE action = 'CREATION_UTILISATEUR';
UPDATE log_audit SET action = 'MODIFICATION_SUPERVISEUR' WHERE action = 'MODIFICATION_UTILISATEUR';
UPDATE log_audit SET action = 'DESACTIVATION_SUPERVISEUR' WHERE action = 'DESACTIVATION_UTILISATEUR';
UPDATE log_audit SET action = 'EXPORT_MESURES' WHERE action = 'EXPORT_DONNEES';
UPDATE log_audit SET action = 'MODIFICATION_CONFIGURATION_PLC' WHERE action = 'MODIFICATION_CONFIGURATION_SYSTEME';

-- ── 2. Suppression de l'ancienne contrainte CHECK ──

ALTER TABLE log_audit
    DROP CONSTRAINT IF EXISTS log_audit_action_check;

-- ── 3. Ajout de la nouvelle contrainte CHECK ──

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
            'VALIDATION_ANOMALIE',
            'EXPORT_MESURES',
            'GENERER_RAPPORT',
            'TELECHARGEMENT_RAPPORT',
            'MODIFICATION_CONFIGURATION_PLC',
            'MODIFICATION_CONFIGURATION_SEUILS'
        ));
