-- V37 : Ajout de IN_APP comme canal valide dans envoi_notification et configuration_destinataire
--
-- Contexte : les contraintes CHECK initiales (V16 et V14) autorisaient uniquement
-- EMAIL, WHATSAPP, PUSH. IN_APP est ajouté pour les notifications temps réel
-- affichées directement dans l'interface (cloche/badge, sans envoi externe).
--
-- Valeurs après migration : EMAIL, WHATSAPP, PUSH, IN_APP
-- Pas de migration de données nécessaire (ajout d'une valeur, aucun renommage).

-- ── envoi_notification ────────────────────────────────────────────────────────

ALTER TABLE envoi_notification
    DROP CONSTRAINT IF EXISTS envoi_notification_canal_check;

ALTER TABLE envoi_notification
    ADD CONSTRAINT envoi_notification_canal_check
        CHECK (canal IN ('EMAIL', 'WHATSAPP', 'PUSH', 'IN_APP'));

-- ── configuration_destinataire ────────────────────────────────────────────────

ALTER TABLE configuration_destinataire
    DROP CONSTRAINT IF EXISTS configuration_destinataire_canal_check;

ALTER TABLE configuration_destinataire
    ADD CONSTRAINT configuration_destinataire_canal_check
        CHECK (canal IN ('EMAIL', 'WHATSAPP', 'PUSH', 'IN_APP'));
