ALTER TABLE seuil_absolu ADD COLUMN actif BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE seuil_absolu ADD COLUMN date_activation TIMESTAMP;
ALTER TABLE seuil_absolu ADD COLUMN date_desactivation TIMESTAMP;

ALTER TABLE seuil_absolu DROP COLUMN updated_at;
ALTER TABLE seuil_absolu DROP COLUMN deleted_at;

DROP INDEX idx_seuil_absolu_metrique;
CREATE INDEX idx_seuil_absolu_point_metrique_actif ON seuil_absolu(id_point_mesure, metrique, actif);

CREATE UNIQUE INDEX idx_seuil_absolu_une_ligne_active ON seuil_absolu(id_point_mesure, metrique) WHERE actif = true;