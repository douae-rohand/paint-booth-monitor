-- V44 : Création de la table abonnement_push_navigateur pour Web Push (VAPID)
--
-- Contexte : Implémentation du canal PUSH pour notifications navigateur natives via Web Push standard.
-- Stocke les abonnements (endpoint + clés de chiffrement) par superviseur.
-- Un superviseur peut avoir plusieurs abonnements (plusieurs navigateurs/appareils).
--
-- Clés VAPID configurées en variables d'environnement (VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT).

CREATE TABLE abonnement_push_navigateur (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    cle_p256dh TEXT NOT NULL,
    cle_auth TEXT NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    user_agent TEXT,
    CONSTRAINT abonnement_push_navigateur_endpoint_unique UNIQUE (endpoint)
);

-- Index pour la recherche par superviseur (fréquente lors de l'envoi de notifications)
CREATE INDEX idx_abonnement_push_navigateur_id_superviseur ON abonnement_push_navigateur(id_superviseur);
