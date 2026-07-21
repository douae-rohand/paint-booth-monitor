CREATE TABLE token_reinitialisation (
    id_token_reset UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    utilise BOOLEAN NOT NULL DEFAULT FALSE,
    date_expiration TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_reset_superviseur ON token_reinitialisation(id_superviseur);