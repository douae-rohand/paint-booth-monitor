CREATE TABLE refresh_token (
    id_refresh_token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500),
    revoque BOOLEAN NOT NULL DEFAULT FALSE,
    date_expiration TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_superviseur ON refresh_token(id_superviseur);