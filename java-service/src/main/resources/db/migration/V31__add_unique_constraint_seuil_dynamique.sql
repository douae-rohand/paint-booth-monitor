ALTER TABLE seuil_dynamique ADD CONSTRAINT uq_seuil_dynamique_point_metrique UNIQUE (id_point_mesure, metrique);

DROP INDEX IF EXISTS idx_seuil_dynamique_metrique;
CREATE INDEX idx_seuil_dynamique_point_metrique ON seuil_dynamique(id_point_mesure, metrique);
