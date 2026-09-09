-- ============================================================
-- db/grants.sql — Document de référence des droits PostgreSQL
-- Projet PFA — Supervision Cabine de Peinture
-- ============================================================
--
-- Ce fichier ne contient AUCUN SQL exécuté automatiquement.
-- Il centralise, à titre de référence humaine, l'état complet
-- des droits accordés aux deux rôles applicatifs (java_service
-- et python_service), quelle que soit la mécanique qui les
-- applique réellement (init.sh, Flyway, ou manuel).
--
-- Rôles applicatifs :
--   java_service   → propriétaire de la logique métier
--   python_service → ingestion PLC, IA, alerting
-- ============================================================


-- ============================================================
-- SECTION 1 — GRANT génériques
-- Appliqués automatiquement via docker/postgres/init.sh
-- (Docker uniquement, au premier démarrage du conteneur)
-- ============================================================

-- Connexion à la base
GRANT CONNECT  ON DATABASE supervision_db TO java_service;
GRANT CONNECT  ON DATABASE supervision_db TO python_service;

-- DDL : java_service doit pouvoir créer des extensions (pgcrypto) et des tables via Flyway
GRANT CREATE   ON DATABASE supervision_db TO java_service;

-- Schéma public
GRANT USAGE, CREATE ON SCHEMA public TO java_service;
GRANT USAGE         ON SCHEMA public TO python_service;

-- Droits génériques sur les tables/séquences déjà présentes au moment de l'init
-- (normalement vide, schéma créé depuis zéro)
GRANT ALL PRIVILEGES                         ON ALL TABLES    IN SCHEMA public TO java_service;
GRANT ALL PRIVILEGES                         ON ALL SEQUENCES IN SCHEMA public TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE         ON ALL TABLES    IN SCHEMA public TO python_service;

-- Règles pour les FUTURES tables/séquences créées par java_service via Flyway
-- FOR ROLE est essentiel : sans lui la règle s'applique au créateur (postgres),
-- pas à java_service qui est le vrai créateur des tables Flyway.
ALTER DEFAULT PRIVILEGES FOR ROLE java_service IN SCHEMA public
    GRANT ALL ON TABLES TO java_service;
ALTER DEFAULT PRIVILEGES FOR ROLE java_service IN SCHEMA public
    GRANT ALL ON SEQUENCES TO java_service;
ALTER DEFAULT PRIVILEGES FOR ROLE java_service IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO python_service;


-- ============================================================
-- SECTION 2 — GRANT fins par table / colonne
-- Appliqués automatiquement via la migration Flyway
--   V49__grants_fine_grained.sql
-- (exécutée par java_service APRÈS création de toutes les tables)
-- ============================================================

-- ── Auth & Accès (propriété java_service) ──────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON superviseur            TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON admin                  TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token          TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_reinitialisation TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_activation       TO java_service;

-- ── Audit (insert-only par conception) ─────────────────────

GRANT SELECT, INSERT ON log_audit TO java_service;

-- ── Notifications (propriété java_service) ─────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON notification               TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON envoi_notification         TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON abonnement_push_navigateur TO java_service;

-- ── Rapports PDF (propriété java_service) ──────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON rapport_pdf TO java_service;

-- ── Chatbot (propriété java_service — tool calling, Spring AI)

GRANT SELECT, INSERT, UPDATE, DELETE ON conversation_chatbot TO java_service;

-- ── Mesures (Python écrit, Java lit) ───────────────────────

GRANT SELECT, INSERT ON mesure TO python_service;
GRANT SELECT         ON mesure TO java_service;

-- ── PredictionIA (Python écrit, Java lit) ──────────────────

GRANT SELECT, INSERT ON prediction_ia TO python_service;
GRANT SELECT         ON prediction_ia TO java_service;

-- ── Alerte — GRANT colonne par colonne (entité hybride) ────
-- Python crée l'alerte, Java met à jour le statut uniquement.

GRANT SELECT, INSERT, UPDATE (statut, updated_at)           ON alerte TO python_service;
GRANT SELECT, UPDATE (statut, updated_at, deleted_at)       ON alerte TO java_service;

-- ── SeuilAbsolu — GRANT colonne par colonne ────────────────
-- Java administre, Python lit pour appliquer à l'ingestion.

GRANT SELECT, INSERT, UPDATE (actif, date_activation, date_desactivation) ON seuil_absolu TO java_service;
GRANT SELECT                                                               ON seuil_absolu TO python_service;

-- ── SeuilDynamique — GRANT colonne par colonne ─────────────
-- Java administre la marge, Python écrit les valeurs calculées.

GRANT SELECT, INSERT, UPDATE (marge_configuree)                              ON seuil_dynamique TO java_service;
GRANT SELECT, UPDATE (valeur_min_calculee, valeur_max_calculee, date_calcul) ON seuil_dynamique TO python_service;

-- ── Configuration & Référentiel ────────────────────────────

-- ConfigurationPLC : Java configure, Python lit au démarrage.
GRANT SELECT, INSERT, UPDATE ON configuration_plc TO java_service;
GRANT SELECT                 ON configuration_plc TO python_service;

-- PointMesure : Java administre, Python lit.
GRANT SELECT, INSERT, UPDATE ON point_mesure TO java_service;
GRANT SELECT                 ON point_mesure TO python_service;

-- ── Séquences ──────────────────────────────────────────────

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO java_service;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO python_service;


-- ============================================================
-- SECTION 3 — GRANT à appliquer MANUELLEMENT en local
-- (hors Docker — développement local sans init.sh)
-- ============================================================
--
-- En local, il n'existe pas de mécanisme équivalent à
-- docker-entrypoint-initdb.d pour exécuter init.sh automatiquement.
-- Ces deux GRANT sont un prérequis de connexion : un rôle doit
-- pouvoir se connecter à la base AVANT que Flyway ou Python
-- ne puissent agir. Ni Flyway ni Python ne peuvent les poser
-- eux-mêmes (ils nécessitent déjà une connexion active).
--
-- À exécuter UNE SEULE FOIS en tant que superuser (postgres) :
--
--   psql -U postgres -d supervision_db
--
-- puis :

GRANT CONNECT ON DATABASE supervision_db TO java_service;
GRANT CONNECT ON DATABASE supervision_db TO python_service;

-- (ainsi que GRANT CREATE ON DATABASE supervision_db TO java_service;
--  pour que Flyway puisse créer les extensions pgcrypto en V1)
