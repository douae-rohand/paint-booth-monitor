-- V49 : GRANT fins par table/colonne pour java_service et python_service
--
-- Contexte : ces GRANT ne peuvent pas être appliqués dans docker/postgres/init.sh
-- car init.sh s'exécute AVANT Flyway (les tables n'existent pas encore à ce stade).
-- En plaçant ces GRANT ici, ils sont exécutés par java_service APRÈS que toutes les
-- tables ont été créées par les migrations V1 à V48.
--
-- Ce fichier couvre l'ensemble des droits fins du projet pour les deux rôles
-- applicatifs (java_service et python_service). Les droits génériques (CONNECT,
-- USAGE ON SCHEMA, ALTER DEFAULT PRIVILEGES) restent dans init.sh car eux peuvent
-- être appliqués dès l'initialisation du conteneur PostgreSQL.
-- ============================================================================

-- ── Domaine Auth & Accès (propriété java_service) ────────────────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON superviseur            TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON admin                  TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token          TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_reinitialisation TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_activation       TO java_service;

-- ── Audit (insert-only par conception) ───────────────────────────────────────

GRANT SELECT, INSERT ON log_audit TO java_service;

-- ── Notifications (propriété java_service) ───────────────────────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON notification               TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON envoi_notification         TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON abonnement_push_navigateur TO java_service;

-- ── Rapports PDF (propriété java_service) ────────────────────────────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON rapport_pdf TO java_service;

-- ── Chatbot (propriété java_service — tool calling, Spring AI) ───────────────

GRANT SELECT, INSERT, UPDATE, DELETE ON conversation_chatbot TO java_service;

-- ── Mesures (Python écrit, Java lit) ─────────────────────────────────────────

GRANT SELECT, INSERT ON mesure TO python_service;
GRANT SELECT         ON mesure TO java_service;

-- ── PredictionIA (Python écrit, Java lit) ────────────────────────────────────

GRANT SELECT, INSERT ON prediction_ia TO python_service;
GRANT SELECT         ON prediction_ia TO java_service;

-- ── Alerte — GRANT colonne par colonne (entité hybride) ──────────────────────
-- Python crée l'alerte à l'ingestion, Java met à jour le statut uniquement.

GRANT SELECT, INSERT, UPDATE (statut, updated_at)           ON alerte TO python_service;
GRANT SELECT, UPDATE (statut, updated_at, deleted_at)       ON alerte TO java_service;

-- ── SeuilAbsolu — GRANT colonne par colonne ───────────────────────────────────
-- Java administre (config Admin), Python lit pour appliquer à l'ingestion.

GRANT SELECT, INSERT, UPDATE (actif, date_activation, date_desactivation) ON seuil_absolu TO java_service;
GRANT SELECT                                                               ON seuil_absolu TO python_service;

-- ── SeuilDynamique — GRANT colonne par colonne ────────────────────────────────
-- Java administre la marge, Python écrit les valeurs calculées.

GRANT SELECT, INSERT, UPDATE (marge_configuree)                              ON seuil_dynamique TO java_service;
GRANT SELECT, UPDATE (valeur_min_calculee, valeur_max_calculee, date_calcul) ON seuil_dynamique TO python_service;

-- ── Configuration & Référentiel ───────────────────────────────────────────────

-- ConfigurationPLC : Java configure (Admin), Python lit au démarrage.
GRANT SELECT, INSERT, UPDATE ON configuration_plc TO java_service;
GRANT SELECT                 ON configuration_plc TO python_service;

-- PointMesure : Java administre (CRUD), Python lit (association mesures).
GRANT SELECT, INSERT, UPDATE ON point_mesure TO java_service;
GRANT SELECT                 ON point_mesure TO python_service;

-- ── Séquences ─────────────────────────────────────────────────────────────────

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO java_service;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO python_service;
