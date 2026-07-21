CREATE TABLE seuil_absolu (
    id_seuil_absolu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_admin UUID NOT NULL REFERENCES admin(id_admin) ON DELETE RESTRICT,
    metrique VARCHAR(20) NOT NULL CHECK (metrique IN ('TEMPERATURE', 'HUMIDITE')),
    valeur_min DECIMAL(6,2) NOT NULL,
    valeur_max DECIMAL(6,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_seuil_absolu_metrique ON seuil_absolu(metrique);