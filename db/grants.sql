-- ============================================================
-- Script de gestion des droits PostgreSQL par service
-- Projet PFA — Supervision Cabine de Peinture
-- À exécuter manuellement via Query Tool (pgAdmin) ou psql,
-- APRÈS que toutes les migrations Flyway aient été appliquées.
-- Rôles déjà créés : java_service, python_service
-- ============================================================

-- ============================================================
-- 1. Droits de connexion de base
-- ============================================================

GRANT CONNECT ON DATABASE supervision_db TO java_service;
GRANT CONNECT ON DATABASE supervision_db TO python_service;

GRANT USAGE ON SCHEMA public TO java_service;
GRANT USAGE ON SCHEMA public TO python_service;

-- ============================================================
-- 2. Domaine Auth & Access (propriété Java, aucun accès Python)
-- ============================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON superviseur TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON admin TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_reinitialisation TO java_service;
GRANT SELECT, INSERT ON log_audit TO java_service;

-- ============================================================
-- 3. Domaine Data & Intelligence (propriété Python)
-- ============================================================

-- Mesure : Python écrit, Java lit seulement (KPIs, historique, export)
GRANT SELECT, INSERT ON mesure TO python_service;
GRANT SELECT ON mesure TO java_service;

-- PredictionIA : Python écrit, Java lit (affichage frontend)
GRANT SELECT, INSERT ON prediction_ia TO python_service;
GRANT SELECT ON prediction_ia TO java_service;

-- DocumentEmbedding + tables de liaison : Python uniquement
GRANT SELECT, INSERT, DELETE ON document_embedding TO python_service;
GRANT SELECT, INSERT, DELETE ON embedding_mesure TO python_service;
GRANT SELECT, INSERT, DELETE ON embedding_alerte TO python_service;

-- ConversationChatbot : Python écrit (par défaut, cf. point ouvert du SCHEMA.md), Java lit
GRANT SELECT, INSERT ON conversation_chatbot TO python_service;
GRANT SELECT ON conversation_chatbot TO java_service;

-- ============================================================
-- 4. Domaine partagé / configuration (propriété Java)
-- ============================================================

-- SeuilAbsolu : Java écrit (config Admin), Python lit (application à l'ingestion)
GRANT SELECT, INSERT, UPDATE, DELETE ON seuil_absolu TO java_service;
GRANT SELECT ON seuil_absolu TO python_service;

-- ConfigurationDestinataire : interne à Java
GRANT SELECT, INSERT, UPDATE, DELETE ON configuration_destinataire TO java_service;

-- Notification (le message) : interne à Java
GRANT SELECT, INSERT, UPDATE, DELETE ON notification TO java_service;

-- EnvoiNotification (l'association destinataire × canal) : interne à Java
GRANT SELECT, INSERT, UPDATE, DELETE ON envoi_notification TO java_service;

-- RapportPDF : interne à Java
GRANT SELECT, INSERT, UPDATE, DELETE ON rapport_pdf TO java_service;

-- ============================================================
-- 5. Entités à cheval — GRANT colonne par colonne
-- ============================================================

-- Alerte : Python crée (à l'ingestion), Java ne modifie que le statut
GRANT SELECT, INSERT ON alerte TO python_service;
GRANT SELECT, UPDATE (statut, updated_at, deleted_at) ON alerte TO java_service;

-- SeuilDynamique : Java administre la config, Python écrit les valeurs calculées
GRANT SELECT, INSERT, UPDATE (metrique, marge_configuree, deleted_at) ON seuil_dynamique TO java_service;
GRANT SELECT, UPDATE (valeur_min_calculee, valeur_max_calculee, date_calcul) ON seuil_dynamique TO python_service;

-- ============================================================
-- 6. Séquences (précaution, même si tout est en UUID ici)
-- ============================================================

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO java_service;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO python_service;

-- ============================================================
-- ConfigurationPLC : Java écrit (Admin configure), Python lit seulement (connexion au démarrage)
-- ============================================================
GRANT SELECT, INSERT, UPDATE ON configuration_plc TO java_service;
GRANT SELECT ON configuration_plc TO python_service;

-- ============================================================
-- 7. Vérification rapide (optionnel, à décommenter pour contrôler)
-- ============================================================

-- SELECT grantee, table_name, privilege_type
-- FROM information_schema.role_table_grants
-- WHERE grantee IN ('java_service', 'python_service')
-- ORDER BY table_name, grantee;