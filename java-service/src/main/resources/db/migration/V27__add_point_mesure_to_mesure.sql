ALTER TABLE mesure
    ADD COLUMN id_point_mesure BIGINT NOT NULL REFERENCES point_mesure(id);