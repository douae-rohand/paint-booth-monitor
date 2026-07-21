CREATE TABLE configuration_plc (
    id_configuration UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_admin UUID NOT NULL REFERENCES admin(id_admin) ON DELETE RESTRICT,
    plc_ip VARCHAR(45) NOT NULL,
    plc_rack INTEGER NOT NULL,
    plc_slot INTEGER NOT NULL,
    plc_polling_interval_ms INTEGER NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_configuration_plc_actif ON configuration_plc(actif) WHERE actif = true;