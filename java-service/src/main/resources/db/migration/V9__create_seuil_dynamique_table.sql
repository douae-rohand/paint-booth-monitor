CREATE TABLE seuil_dynamique (
    id_seuil_dynamique UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_admin UUID NOT NULL REFERENCES admin(id_admin) ON DELETE RESTRICT,
    metrique VARCHAR(20) NOT NULL CHECK (metrique IN ('TEMPERATURE', 'HUMIDITE')),
    valeur_min_calculee DECIMAL(6,2),
    valeur_max_calculee DECIMAL(6,2),
    marge_configuree DECIMAL(6,2) NOT NULL,
    date_calcul TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_seuil_dynamique_metrique ON seuil_dynamique(metrique);