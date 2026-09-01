-- V42 : Suppression de RAPPORT_GENERE des contraintes CHECK type_evenement
--
-- Contexte : les rapports PDF sont générés à la demande et ne déclenchent plus de notification.
-- La valeur RAPPORT_GENERE n'est plus utilisée dans TypeEvenement.java et doit être retirée
-- des contraintes CHECK pour garantir la cohérence entre le code Java et le schéma DB.
--
-- Note : La table configuration_destinataire a été supprimée dans V38, seule notification reste à mettre à jour.
--
-- Nouvelles valeurs : ALERTE_CREE, ALERTE_RESOLU, COMPTE_ACTIVEE, CONFIG_SEUILS_MODIFIE
--
-- Ordre obligatoire : DROP contrainte → ADD nouvelle contrainte.

-- ── 1. DROP de l'ancienne contrainte CHECK ──

ALTER TABLE notification
    DROP CONSTRAINT IF EXISTS notification_type_evenement_check;

-- ── 2. ADD de la nouvelle contrainte CHECK sans RAPPORT_GENERE ─────────

ALTER TABLE notification
    ADD CONSTRAINT notification_type_evenement_check
        CHECK (type_evenement IN (
            'ALERTE_CREE',
            'ALERTE_RESOLU',
            'COMPTE_ACTIVEE',
            'CONFIG_SEUILS_MODIFIE'
        ));
