-- V34__make_mot_de_passe_hash_nullable.sql
ALTER TABLE superviseur ALTER COLUMN mot_de_passe_hash DROP NOT NULL;