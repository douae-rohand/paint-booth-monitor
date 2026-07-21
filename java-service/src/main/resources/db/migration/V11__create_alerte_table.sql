CREATE TABLE alerte (
    id_alerte UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_mesure UUID NOT NULL REFERENCES mesure(id_mesure) ON DELETE RESTRICT,
    metrique VARCHAR(20) NOT NULL CHECK (metrique IN ('TEMPERATURE', 'HUMIDITE')),
    type_alerte VARCHAR(20) NOT NULL CHECK (type_alerte IN ('SEUIL_ABSOLU', 'SEUIL_DYNAMIQUE', 'DERIVE_IA')),
    severite VARCHAR(10) NOT NULL CHECK (severite IN ('FAIBLE', 'MOYENNE', 'CRITIQUE')),
    statut VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'RESOLUE')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_alerte_mesure ON alerte(id_mesure);
CREATE INDEX idx_alerte_statut ON alerte(statut);
CREATE INDEX idx_alerte_severite ON alerte(severite);