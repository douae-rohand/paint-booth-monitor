CREATE TABLE mesure (
    id_mesure UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metrique VARCHAR(20) NOT NULL CHECK (metrique IN ('TEMPERATURE', 'HUMIDITE')),
    valeur DECIMAL(6,2) NOT NULL,
    identifiant_caisse VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_mesure_metrique ON mesure(metrique);
CREATE INDEX idx_mesure_created_at ON mesure(created_at);
CREATE INDEX idx_mesure_identifiant_caisse ON mesure(identifiant_caisse) WHERE identifiant_caisse IS NOT NULL;