-- V38 : Suppression de la table configuration_destinataire
--
-- Contexte : la configuration des destinataires par canal/événement était prévue
-- via cette table, mais n'a jamais été utilisée ni exposée en UI.
-- La décision actée est de gérer ce mapping en dur dans le code Java
-- (NotificationDispatchServiceImpl), ce qui est plus simple et suffisant
-- pour ce projet.
--
-- Les index associés sont supprimés automatiquement par DROP TABLE CASCADE.

DROP TABLE IF EXISTS configuration_destinataire CASCADE;
