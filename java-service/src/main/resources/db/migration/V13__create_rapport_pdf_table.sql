CREATE TABLE rapport_pdf (
    id_rapport UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE RESTRICT,
    type_rapport VARCHAR(20) NOT NULL CHECK (type_rapport IN ('JOURNALIER', 'HEBDOMADAIRE', 'MENSUEL', 'PERSONNALISE')),
    periode_debut DATE NOT NULL,
    periode_fin DATE NOT NULL,
    chemin_fichier VARCHAR(500),
    statut_generation VARCHAR(20) NOT NULL DEFAULT 'EN_COURS' CHECK (statut_generation IN ('EN_COURS', 'TERMINE', 'ECHEC')),
    generated_at TIMESTAMP,
    date_rapport TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rapport_superviseur ON rapport_pdf(id_superviseur);