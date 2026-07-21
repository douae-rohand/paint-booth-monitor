CREATE TABLE document_embedding (
    id_embedding UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_document_source UUID NOT NULL,
    numero_chunk INTEGER NOT NULL DEFAULT 0,
    contenu_texte TEXT NOT NULL,
    embedding VECTOR(1536),
    date_indexation TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_embedding_document_source ON document_embedding(id_document_source);
-- Index vectoriel (approximation rapide de similarité, à ajuster selon le volume réel)
CREATE INDEX idx_embedding_vector ON document_embedding USING ivfflat (embedding vector_cosine_ops);