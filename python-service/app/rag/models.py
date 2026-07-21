"""
Module: rag
SQLAlchemy 2.x ORM models for the RAG (Retrieval-Augmented Generation) module.

This module is 100% Python-owned. java-service has NO access (no read, no write)
to DocumentEmbedding, EmbeddingMesure, or EmbeddingAlerte.
java-service reads ConversationChatbot for frontend history display only.

Entities defined here:
- DocumentEmbedding   → 100% Python, used for vector similarity search
- EmbeddingMesure     → Pure join table (DocumentEmbedding ↔ Mesure)
- EmbeddingAlerte     → Pure join table (DocumentEmbedding ↔ Alerte)
- ConversationChatbot → Python writes (RAG response), java-service reads (history)
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional
from uuid import UUID as PythonUUID

from pgvector.sqlalchemy import Vector
from sqlalchemy import ForeignKey, Integer, PrimaryKeyConstraint, Text, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class DocumentEmbedding(Base):
    """
    Module: rag
    Table SQL: document_embedding

    Written BY: python-service (RAG indexation pipeline).
    Read BY: python-service only (vector similarity search).
    java-service: NO ACCESS whatsoever.

    id_document_source groups chunks from the same source document (no FK — the
    source may be an external URL or file, not a DB entity).
    embedding uses pgvector (Vector(1536)) for cosine similarity queries.
    """

    __tablename__ = "document_embedding"

    id_embedding: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    # Groups chunks of the same source document — not a FK, source is external.
    id_document_source: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    numero_chunk: Mapped[int] = mapped_column(Integer, nullable=False)
    contenu_texte: Mapped[str] = mapped_column(Text, nullable=False)
    embedding: Mapped[list] = mapped_column(Vector(1536), nullable=False)
    date_indexation: Mapped[datetime] = mapped_column(
        nullable=False, server_default=text("now()")
    )

    # Relationships to join tables
    mesures: Mapped[list["EmbeddingMesure"]] = relationship(
        back_populates="document", cascade="all, delete-orphan"
    )
    alertes: Mapped[list["EmbeddingAlerte"]] = relationship(
        back_populates="document", cascade="all, delete-orphan"
    )


class EmbeddingMesure(Base):
    """
    Module: rag
    Table SQL: embedding_mesure

    Pure many-to-many join table between DocumentEmbedding and mesure.
    No surrogate PK — composite PK on (id_embedding, id_mesure).

    Written BY: python-service only.
    java-service: NO ACCESS.

    id_mesure is a FK to mesure (owned by plc module) — both sides are
    Python-owned, so FK is safe here.
    """

    __tablename__ = "embedding_mesure"

    id_embedding: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("document_embedding.id_embedding", ondelete="CASCADE"),
        nullable=False,
    )
    id_mesure: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("mesure.id_mesure", ondelete="CASCADE"),
        nullable=False,
    )

    __table_args__ = (
        PrimaryKeyConstraint("id_embedding", "id_mesure"),
    )

    document: Mapped["DocumentEmbedding"] = relationship(back_populates="mesures")


class EmbeddingAlerte(Base):
    """
    Module: rag
    Table SQL: embedding_alerte

    Pure many-to-many join table between DocumentEmbedding and alerte.
    No surrogate PK — composite PK on (id_embedding, id_alerte).

    Written BY: python-service only.
    java-service: NO ACCESS.

    id_alerte is a FK to alerte. Note: alerte is a hybrid table (Python inserts,
    Java updates statut), but the FK here is safe because Python is the writer
    for the alerte rows being referenced.
    """

    __tablename__ = "embedding_alerte"

    id_embedding: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("document_embedding.id_embedding", ondelete="CASCADE"),
        nullable=False,
    )
    id_alerte: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("alerte.id_alerte", ondelete="CASCADE"),
        nullable=False,
    )

    __table_args__ = (
        PrimaryKeyConstraint("id_embedding", "id_alerte"),
    )

    document: Mapped["DocumentEmbedding"] = relationship(back_populates="alertes")


class ConversationChatbot(Base):
    """
    Module: rag
    Table SQL: conversation_chatbot

    Written BY: python-service — after generating the RAG response (question +
    reponse are persisted together once the LLM answers).
    Read BY: java-service — for displaying the conversation history in the frontend
    (see CDC section 13.2, "action utilisateur" flow).

    id_superviseur is stored as a plain UUID. No ORM relationship toward
    Superviseur, which is managed exclusively by java-service.
    """

    __tablename__ = "conversation_chatbot"

    id_conversation: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    # Plain UUID — no FK/relationship toward java-service's superviseur table.
    id_superviseur: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    question: Mapped[str] = mapped_column(Text, nullable=False)
    reponse: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    date_echange: Mapped[datetime] = mapped_column(
        nullable=False, server_default=text("now()")
    )
