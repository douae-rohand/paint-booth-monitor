CREATE TABLE prediction_ia (
    id_prediction UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_mesure UUID NOT NULL REFERENCES mesure(id_mesure) ON DELETE RESTRICT,
    modele_utilise VARCHAR(20) NOT NULL CHECK (modele_utilise IN ('ISOLATION_FOREST', 'REGRESSION')),
    valeur_predite DECIMAL(6,2),
    valeur_reelle DECIMAL(6,2),
    est_anomalie BOOLEAN,
    date_prediction TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_prediction_mesure ON prediction_ia(id_mesure);