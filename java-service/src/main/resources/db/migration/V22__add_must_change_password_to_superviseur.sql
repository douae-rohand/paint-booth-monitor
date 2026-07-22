-- V22__add_must_change_password_to_superviseur.sql
ALTER TABLE superviseur
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;