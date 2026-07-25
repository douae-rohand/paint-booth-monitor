ALTER TABLE seuil_absolu
    ADD COLUMN id_point_mesure BIGINT NOT NULL REFERENCES point_mesure(id);