-- V41 : Renommage de rapport_pdf.chemin_fichier → objet_minio_storage_key
--
-- Contexte : chemin_fichier portait un nom hérité d'une logique de stockage disque local.
-- Depuis V40, un COMMENT documente qu'il stocke une clé d'objet MinIO. Le renommage
-- rend l'intention explicite dans le schéma lui-même, sans dépendre du COMMENT.
--
-- Aucune autre colonne n'est modifiée. Type VARCHAR(500) et nullabilité inchangés.
-- Aucune donnée réelle à préserver (aucun rapport généré en production à ce stade).
--
-- Références vérifiées avant application : aucune requête native, DTO, service, ni test
-- ne référence chemin_fichier en dehors de l'entité RapportPDF — renommage sans risque.

-- ── 1. Supprimer le COMMENT existant avant le renommage ──────────────────────
-- PostgreSQL lie le COMMENT au nom de colonne ; le supprimer avant évite toute ambiguïté.

COMMENT ON COLUMN rapport_pdf.chemin_fichier IS NULL;

-- ── 2. Renommage ──────────────────────────────────────────────────────────────

ALTER TABLE rapport_pdf
    RENAME COLUMN chemin_fichier TO objet_minio_storage_key;
