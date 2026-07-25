CREATE TABLE point_mesure (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    type_emplacement VARCHAR(20) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT true,
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_type_emplacement CHECK (type_emplacement IN ('CABINE', 'ETUVE'))
);

-- Données initiales : 1 cabine (température + humidité) + 5 zones d'étuve (température)
INSERT INTO point_mesure (nom, type_emplacement) VALUES
    ('Cabine d''après', 'CABINE'),
    ('Étuve - Zone 1', 'ETUVE'),
    ('Étuve - Zone 2', 'ETUVE'),
    ('Étuve - Zone 3', 'ETUVE'),
    ('Étuve - Zone 4', 'ETUVE'),
    ('Étuve - Zone 5', 'ETUVE');