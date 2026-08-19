-- V35 : Ajout des colonnes de retry sur envoi_notification
-- Nécessaire pour le pattern outbox SendGrid :
--   tentatives    : compteur d'essais d'envoi (incrémenté à chaque échec)
--   derniere_erreur : message d'erreur exact renvoyé par SendGrid (code + body)
--                    utile pour diagnostiquer sans fouiller les logs applicatifs

ALTER TABLE envoi_notification
    ADD COLUMN tentatives INT NOT NULL DEFAULT 0;

ALTER TABLE envoi_notification
    ADD COLUMN derniere_erreur TEXT;

-- Index partiel pour que le worker EmailWorkerService soit efficace :
-- seules les lignes EMAIL EN_ATTENTE sont scannées à chaque cycle scheduled.
CREATE INDEX idx_envoi_email_en_attente
    ON envoi_notification (canal, statut_envoi)
    WHERE canal = 'EMAIL' AND statut_envoi = 'EN_ATTENTE';
