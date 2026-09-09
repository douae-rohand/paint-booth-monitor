-- V48 : Suppression des tables RAG vectoriel
--
-- Contexte : l'approche RAG vectoriel (embeddings + pgvector + LangChain) a été
-- abandonnée. Le futur chatbot sera un module Java à appel d'outils (Spring AI),
-- sans besoin de stockage vectoriel. La table conversation_chatbot est conservée
-- car elle restera utilisée par le nouveau module chatbot Java.
--
-- Ordre : tables de liaison avant la table parente (contraintes FK).

-- 1. Tables de liaison (dépendent de document_embedding)
DROP TABLE IF EXISTS embedding_alerte    CASCADE;
DROP TABLE IF EXISTS embedding_mesure    CASCADE;

-- 2. Table parente
DROP TABLE IF EXISTS document_embedding  CASCADE;
