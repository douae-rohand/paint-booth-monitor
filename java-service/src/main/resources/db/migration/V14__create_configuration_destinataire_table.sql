CREATE TABLE configuration_destinataire (
    id_configuration UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_admin UUID NOT NULL REFERENCES admin(id_admin) ON DELETE RESTRICT,
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    type_evenement VARCHAR(30) NOT NULL CHECK (type_evenement IN ('ALERTE', 'RAPPORT_GENERE', 'COMPTE_CREE', 'SEUIL_MODIFIE')),
    severite VARCHAR(10) CHECK (severite IN ('FAIBLE', 'MOYENNE', 'CRITIQUE')),
    canal VARCHAR(10) NOT NULL CHECK (canal IN ('EMAIL', 'WHATSAPP', 'PUSH')),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_configuration TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_config_destinataire_superviseur ON configuration_destinataire(id_superviseur);
CREATE INDEX idx_config_destinataire_type_severite ON configuration_destinataire(type_evenement, severite);