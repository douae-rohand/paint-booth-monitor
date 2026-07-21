CREATE TABLE embedding_alerte (
    id_embedding UUID NOT NULL REFERENCES document_embedding(id_embedding) ON DELETE CASCADE,
    id_alerte UUID NOT NULL REFERENCES alerte(id_alerte) ON DELETE CASCADE,
    PRIMARY KEY (id_embedding, id_alerte)
);