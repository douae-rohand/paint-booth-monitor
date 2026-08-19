-- V39 : Remplacement de notification.contenu (TEXT) par notification.donnees_evenement (JSONB)
--
-- Contexte : la colonne contenu stockait un texte pré-formaté pour IN_APP, ignoré par EMAIL
-- qui reconstruisait son propre HTML depuis les données brutes de l'alerte. Cette asymétrie
-- créait une désynchronisation entre les deux canaux (même source, deux mises en forme
-- incompatibles ne partageant pas de données structurées communes).
--
-- Décision actée : donnees_evenement (JSONB) stocke les données brutes de l'événement
-- (metrique, typeAlerte, severite, nomPointMesure, etc.), permettant à chaque canal de
-- produire sa propre mise en forme à partir de la même source structurée.
--
-- Migration de données : non effectuée intentionnellement.
-- Le texte libre de contenu n'est pas extractible en JSON structuré de façon fiable.
-- Les notifications existantes perdront leur contenu détaillé après cette migration —
-- acceptable en environnement de développement (aucune donnée de production réelle).
--
-- Ordre : ADD COLUMN avant DROP COLUMN (discipline cohérente avec les migrations précédentes,
-- même si aucune dépendance entre les deux opérations ici).

ALTER TABLE notification
    ADD COLUMN donnees_evenement JSONB;

ALTER TABLE notification
    DROP COLUMN contenu;
