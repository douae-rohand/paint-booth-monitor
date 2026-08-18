-- V36 : Mise à jour des valeurs de type_evenement sur notification et configuration_destinataire
--
-- Contexte : la contrainte CHECK initiale (V14/V15) utilisait des valeurs trop génériques.
-- Les nouvelles valeurs sont plus explicites et permettent de distinguer création/résolution d'alerte.
--
-- Mapping de migration des données existantes :
--   ALERTE        → ALERTE_CREE            (assume création par défaut, résolution indistinguable rétroactivement)
--   COMPTE_CREE   → COMPTE_ACTIVEE         (l'événement notifiable est l'activation, pas la création Admin)
--   SEUIL_MODIFIE → CONFIG_SEUILS_MODIFIE  (nom plus explicite)
--   RAPPORT_GENERE → RAPPORT_GENERE        (inchangé)
--
-- Nouvelles valeurs : ALERTE_CREE, ALERTE_RESOLU, RAPPORT_GENERE, COMPTE_ACTIVEE, CONFIG_SEUILS_MODIFIE
--
-- Ordre obligatoire : DROP contraintes → UPDATE données → ADD nouvelles contraintes.
-- Les UPDATE vers les nouvelles valeurs violeraient l'ancienne CHECK si les DROP passaient après.

-- ── 1. DROP des anciennes contraintes CHECK (avant toute modification de données) ──

ALTER TABLE notification
    DROP CONSTRAINT IF EXISTS notification_type_evenement_check;

ALTER TABLE configuration_destinataire
    DROP CONSTRAINT IF EXISTS configuration_destinataire_type_evenement_check;

-- ── 2. Migrer les données existantes (maintenant sans contrainte active) ──────

UPDATE notification
SET type_evenement = 'ALERTE_CREE'
WHERE type_evenement = 'ALERTE';

UPDATE notification
SET type_evenement = 'COMPTE_ACTIVEE'
WHERE type_evenement = 'COMPTE_CREE';

UPDATE notification
SET type_evenement = 'CONFIG_SEUILS_MODIFIE'
WHERE type_evenement = 'SEUIL_MODIFIE';

UPDATE configuration_destinataire
SET type_evenement = 'ALERTE_CREE'
WHERE type_evenement = 'ALERTE';

UPDATE configuration_destinataire
SET type_evenement = 'COMPTE_ACTIVEE'
WHERE type_evenement = 'COMPTE_CREE';

UPDATE configuration_destinataire
SET type_evenement = 'CONFIG_SEUILS_MODIFIE'
WHERE type_evenement = 'SEUIL_MODIFIE';

-- ── 3. ADD des nouvelles contraintes CHECK avec les 5 valeurs exactes ─────────

ALTER TABLE notification
    ADD CONSTRAINT notification_type_evenement_check
        CHECK (type_evenement IN (
            'ALERTE_CREE',
            'ALERTE_RESOLU',
            'RAPPORT_GENERE',
            'COMPTE_ACTIVEE',
            'CONFIG_SEUILS_MODIFIE'
        ));

ALTER TABLE configuration_destinataire
    ADD CONSTRAINT configuration_destinataire_type_evenement_check
        CHECK (type_evenement IN (
            'ALERTE_CREE',
            'ALERTE_RESOLU',
            'RAPPORT_GENERE',
            'COMPTE_ACTIVEE',
            'CONFIG_SEUILS_MODIFIE'
        ));
