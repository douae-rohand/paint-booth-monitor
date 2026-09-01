-- V43 : Suppression de WHATSAPP des contraintes CHECK canal
--
-- Contexte : le canal WHATSAPP n'est plus utilisé et doit être retiré de l'enum Canal.java.
-- Les contraintes CHECK doivent être mises à jour pour garantir la cohérence entre le code Java et le schéma DB.
--
-- Note : La table configuration_destinataire a été supprimée dans V38, seule envoi_notification reste à mettre à jour.
--
-- Nouvelles valeurs : EMAIL, PUSH, IN_APP
--
-- Ordre obligatoire : DROP contrainte → ADD nouvelle contrainte.

-- ── 1. DROP de l'ancienne contrainte CHECK ──

ALTER TABLE envoi_notification
    DROP CONSTRAINT IF EXISTS envoi_notification_canal_check;

-- ── 2. ADD de la nouvelle contrainte CHECK sans WHATSAPP ─────────

ALTER TABLE envoi_notification
    ADD CONSTRAINT envoi_notification_canal_check
        CHECK (canal IN ('EMAIL', 'PUSH', 'IN_APP'));
