CREATE TABLE conversation_chatbot (
    id_conversation UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_superviseur UUID NOT NULL REFERENCES superviseur(id_superviseur) ON DELETE CASCADE,
    question TEXT NOT NULL,
    reponse TEXT,
    date_echange TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_superviseur ON conversation_chatbot(id_superviseur);