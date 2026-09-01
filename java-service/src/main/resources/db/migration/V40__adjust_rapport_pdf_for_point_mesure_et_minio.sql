-- V40 : Ajustements de rapport_pdf pour le module reports
--
-- Contexte : la table rapport_pdf (V13) a été créée comme squelette lors de la
-- modélisation initiale. Ce prompt ajoute les colonnes nécessaires à l'implémentation
-- réelle : lien vers un point de mesure, stockage MinIO, et précision temporelle.
--
-- Modifications :
--   1. Ajout de id_point_mesure (BIGINT, FK point_mesure) — même pattern que V27/V28
--   2. Conversion de periode_debut/periode_fin de DATE vers TIMESTAMP
--   3. Ajout de nom_fichier (VARCHAR 255, nullable — rempli après génération réussie)
--   4. Ajout de taille_fichier (BIGINT, nullable — rempli après génération réussie)
--   5. COMMENT sur chemin_fichier pour documenter qu'il stocke une clé MinIO

-- ── 1. Ajout de id_point_mesure ───────────────────────────────────────────────
-- Même pattern que V27 (mesure) et V28 (seuil_absolu)

ALTER TABLE rapport_pdf
    ADD COLUMN id_point_mesure BIGINT NOT NULL REFERENCES point_mesure(id);

CREATE INDEX idx_rapport_point_mesure ON rapport_pdf(id_point_mesure);

-- ── 2. Conversion DATE → TIMESTAMP pour periode_debut / periode_fin ───────────
-- Nécessaire pour les rapports personnalisés sur des fenêtres horaires précises.
-- La conversion est sans perte : DATE '2026-08-08' → TIMESTAMP '2026-08-08 00:00:00'.
-- Aucune donnée réelle à préserver (aucun rapport généré en production à ce stade).

ALTER TABLE rapport_pdf
    ALTER COLUMN periode_debut TYPE TIMESTAMP USING periode_debut::TIMESTAMP;

ALTER TABLE rapport_pdf
    ALTER COLUMN periode_fin TYPE TIMESTAMP USING periode_fin::TIMESTAMP;

-- ── 3. Ajout de nom_fichier ───────────────────────────────────────────────────
-- Nom du fichier PDF tel qu'il sera présenté à l'utilisateur (ex: rapport-cabine-2026-08.pdf).
-- Rempli uniquement après génération réussie (statut_generation = 'TERMINE').

ALTER TABLE rapport_pdf
    ADD COLUMN nom_fichier VARCHAR(255);

-- ── 4. Ajout de taille_fichier ────────────────────────────────────────────────
-- Taille en octets du PDF généré. Rempli uniquement après génération réussie.

ALTER TABLE rapport_pdf
    ADD COLUMN taille_fichier BIGINT;

-- ── 5. Documentation de chemin_fichier (clé objet MinIO) ─────────────────────
-- chemin_fichier stocke désormais une clé d'objet MinIO, pas un chemin disque local.
-- Format attendu : rapports/{annee}/{mois}/{id_rapport}.pdf
-- Ex : rapports/2026/08/550e8400-e29b-41d4-a716-446655440000.pdf

COMMENT ON COLUMN rapport_pdf.chemin_fichier IS
    'Clé d''objet MinIO (pas un chemin disque local). '
    'Format : rapports/{annee}/{mois}/{id_rapport}.pdf — '
    'ex: rapports/2026/08/550e8400-e29b-41d4-a716-446655440000.pdf. '
    'Rempli uniquement après génération réussie (statut_generation = TERMINE).';
