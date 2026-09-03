-- ============================================================
-- Script de gestion des droits PostgreSQL par service
-- Projet PFA — Supervision Cabine de Peinture
--
-- À exécuter manuellement via psql ou pgAdmin,
-- APRÈS que toutes les migrations Flyway aient été appliquées
-- (la dernière migration en date est V47).
--
-- Rôles supposés déjà créés par docker/postgres/init.sh :
--   java_service, python_service
--
-- Principe appliqué : moindre privilège par service.
--   java_service  → propriétaire de la logique métier
--   python_service → ingestion des mesures, IA, chatbot
-- ============================================================

-- ============================================================
-- 1. Droits de connexion et de schéma
-- ============================================================

GRANT CONNECT ON DATABASE supervision_db TO java_service;
GRANT CONNECT ON DATABASE supervision_db TO python_service;

GRANT USAGE ON SCHEMA public TO java_service;
GRANT USAGE ON SCHEMA public TO python_service;

-- ============================================================
-- 2. Domaine Auth & Accès (propriété Java, Python sans accès)
-- ============================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON superviseur             TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON admin                   TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token           TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_reinitialisation  TO java_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON token_activation        TO java_service;

-- ============================================================
-- 3. Audit (insert-only par conception — pas d'UPDATE ni DELETE)
-- ============================================================

GRANT SELECT, INSERT ON log_audit TO java_service;
-- Python n'a pas accès aux logs d'audit

-- ============================================================
-- 4. Notifications (propriété Java)
-- ============================================================

-- notification : le message (titre + donnees_evenement JSONB)
GRANT SELECT, INSERT, UPDATE, DELETE ON notification            TO java_service;

-- envoi_notification : association destinataire × canal (EMAIL, PUSH, IN_APP)
-- Note : canal WHATSAPP supprimé en V43
GRANT SELECT, INSERT, UPDATE, DELETE ON envoi_notification      TO java_service;

-- abonnement_push_navigateur : abonnements Web Push VAPID (V47)
GRANT SELECT, INSERT, UPDATE, DELETE ON abonnement_push_navigateur TO java_service;
-- Python n'a pas accès aux abonnements push

-- ============================================================
-- 5. Rapports PDF (propriété Java)
-- ============================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON rapport_pdf TO java_service;

-- ============================================================
-- 6. Domaine Data & Intelligence (propriété Python)
-- ============================================================

-- Mesure : Python écrit à l'ingestion, Java lit (KPIs, historique, export)
GRANT SELECT, INSERT ON mesure TO python_service;
GRANT SELECT         ON mesure TO java_service;

-- PredictionIA : Python écrit, Java lit (affichage frontend)
GRANT SELECT, INSERT ON prediction_ia TO python_service;
GRANT SELECT         ON prediction_ia TO java_service;

-- DocumentEmbedding + tables de liaison : Python uniquement (RAG)
GRANT SELECT, INSERT, DELETE ON document_embedding  TO python_service;
GRANT SELECT, INSERT, DELETE ON embedding_mesure    TO python_service;
GRANT SELECT, INSERT, DELETE ON embedding_alerte    TO python_service;

-- ConversationChatbot : Python écrit (sessions RAG), Java lit (affichage)
GRANT SELECT, INSERT ON conversation_chatbot TO python_service;
GRANT SELECT         ON conversation_chatbot TO java_service;

-- ============================================================
-- 7. Entités à cheval — GRANT colonne par colonne
-- ============================================================

-- Alerte : Python crée (ingestion) et résout (statut RESOLUE),
--          Java peut aussi modifier le statut (résolution manuelle)
GRANT SELECT, INSERT, UPDATE (statut, updated_at)              ON alerte TO python_service;
GRANT SELECT, UPDATE (statut, updated_at, deleted_at)          ON alerte TO java_service;

-- SeuilAbsolu : Java administre (config Admin), Python lit (application à l'ingestion)
GRANT SELECT, INSERT, UPDATE (actif, date_activation, date_desactivation) ON seuil_absolu TO java_service;
GRANT SELECT                                                               ON seuil_absolu TO python_service;

-- SeuilDynamique : Java administre la marge, Python écrit les valeurs calculées
GRANT SELECT, INSERT, UPDATE (marge_configuree)                            ON seuil_dynamique TO java_service;
GRANT SELECT, UPDATE (valeur_min_calculee, valeur_max_calculee, date_calcul) ON seuil_dynamique TO python_service;

-- ============================================================
-- 8. Configuration & Référentiel (propriété Java)
-- ============================================================

-- ConfigurationPLC : Java configure (Admin), Python lit au démarrage
GRANT SELECT, INSERT, UPDATE ON configuration_plc TO java_service;
GRANT SELECT                 ON configuration_plc TO python_service;

-- PointMesure : Java administre (CRUD), Python lit (association mesures)
GRANT SELECT, INSERT, UPDATE ON point_mesure TO java_service;
GRANT SELECT                 ON point_mesure TO python_service;

-- ============================================================
-- 9. Séquences
-- ============================================================

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO java_service;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO python_service;

-- ============================================================
-- 10. DEFAULT PRIVILEGES — droits automatiques sur les futures tables
--     Utile si de nouvelles migrations ajoutent des tables après
--     l'exécution de ce script.
-- ============================================================

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO java_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT ON TABLES TO python_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO java_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO python_service;

-- ============================================================
-- 11. Vérification rapide (décommenter pour contrôler)
-- ============================================================

-- SELECT grantee, table_name, privilege_type
-- FROM information_schema.role_table_grants
-- WHERE grantee IN ('java_service', 'python_service')
-- ORDER BY table_name, grantee, privilege_type;
