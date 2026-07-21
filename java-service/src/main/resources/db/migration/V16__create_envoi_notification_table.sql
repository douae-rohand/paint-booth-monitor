CREATE TABLE envoi_notification (
    id_envoi UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_notification UUID NOT NULL REFERENCES notification(id_notification) ON DELETE CASCADE,
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    canal VARCHAR(10) NOT NULL CHECK (canal IN ('EMAIL', 'WHATSAPP', 'PUSH')),
    statut_envoi VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut_envoi IN ('ENVOYE', 'ECHEC', 'EN_ATTENTE')),
    date_envoi TIMESTAMP,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    date_lecture TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_envoi_notification ON envoi_notification(id_notification);
CREATE INDEX idx_envoi_superviseur ON envoi_notification(id_superviseur);
CREATE INDEX idx_envoi_statut ON envoi_notification(statut_envoi);
CREATE INDEX idx_envoi_lu ON envoi_notification(lu) WHERE lu = false;