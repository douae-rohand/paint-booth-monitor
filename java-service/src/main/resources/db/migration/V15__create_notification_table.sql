CREATE TABLE notification (
    id_notification UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_alerte UUID REFERENCES alerte(id_alerte) ON DELETE SET NULL,
    type_evenement VARCHAR(30) NOT NULL CHECK (type_evenement IN ('ALERTE', 'RAPPORT_GENERE', 'COMPTE_CREE', 'SEUIL_MODIFIE')),
    titre VARCHAR(255),
    contenu TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_notification_alerte ON notification(id_alerte);
CREATE INDEX idx_notification_type ON notification(type_evenement);