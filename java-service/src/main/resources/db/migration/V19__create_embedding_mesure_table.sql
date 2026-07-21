CREATE TABLE embedding_mesure (
    id_embedding UUID NOT NULL REFERENCES document_embedding(id_embedding) ON DELETE CASCADE,
    id_mesure UUID NOT NULL REFERENCES mesure(id_mesure) ON DELETE CASCADE,
    PRIMARY KEY (id_embedding, id_mesure)
);