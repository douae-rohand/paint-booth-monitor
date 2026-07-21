"""
Module: alerting
SQLAlchemy 2.x ORM models for the alerting module.

Enums are defined inline at the top of this file (avoids a separate enums.py
for a self-contained module with no enum reuse across files).

Entities defined here:
- SeuilAbsolu   → READ-ONLY from Python
- SeuilDynamique → HYBRID (Python writes calculated fields only)
- Alerte         → HYBRID (Python inserts, Java updates statut)
"""

from __future__ import annotations

import enum
from datetime import datetime
from decimal import Decimal
from typing import Optional
from uuid import UUID as PythonUUID

from sqlalchemy import Boolean, Enum as SAEnum, Numeric, text, update
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


# ── Enums ──────────────────────────────────────────────────────────────────────

class Metrique(str, enum.Enum):
    TEMPERATURE = "TEMPERATURE"
    HUMIDITE = "HUMIDITE"


class TypeAlerte(str, enum.Enum):
    SEUIL_ABSOLU = "SEUIL_ABSOLU"
    SEUIL_DYNAMIQUE = "SEUIL_DYNAMIQUE"
    DERIVE_IA = "DERIVE_IA"


class Severite(str, enum.Enum):
    FAIBLE = "FAIBLE"
    MOYENNE = "MOYENNE"
    CRITIQUE = "CRITIQUE"


class StatutAlerte(str, enum.Enum):
    ACTIVE = "ACTIVE"
    RESOLUE = "RESOLUE"


# ── Models ─────────────────────────────────────────────────────────────────────

class SeuilAbsolu(Base):
    """
    Module: alerting
    Table SQL: seuil_absolu

    Written BY: java-service (Admin configuration only).
    Read BY: python-service (read-only — used at ingestion to check if a measured
             value exceeds the absolute min/max bounds for a given metrique).

    IMPORTANT: NEVER call session.add() or session.commit() on this entity from
    python-service. This is a configuration table owned by java-service. Python
    only SELECTs from it.

    id_admin is stored as a plain UUID. No ORM relationship toward Superviseur/Admin
    tables managed by java-service.
    """

    __tablename__ = "seuil_absolu"

    id_seuil_absolu: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    id_admin: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    metrique: Mapped[str] = mapped_column(
        SAEnum(Metrique, name="metrique_seuil_absolu_enum", create_type=False),
        nullable=False,
    )
    valeur_min: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    valeur_max: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        nullable=False, server_default=text("now()")
    )
    updated_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)
    deleted_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)


class SeuilDynamique(Base):
    """
    Module: alerting
    Table SQL: seuil_dynamique

    HYBRID entity — shared between java-service and python-service.

    Written BY java-service: 'metrique', 'marge_configuree' (Admin configuration),
                              and soft-delete 'deleted_at'.
    Written BY python-service: ONLY 'valeur_min_calculee', 'valeur_max_calculee',
                               'date_calcul' (rolling mean/std recalculation).

    IMPORTANT: Python must NEVER overwrite 'metrique' or 'marge_configuree'.
    Always use a targeted UPDATE on the 3 calculated columns only (e.g., using
    sqlalchemy's update() construct with specific column targets), never reload
    the full object and save it, to avoid overwriting java-side config fields.

    Example safe update pattern:
        session.execute(
            update(SeuilDynamique)
            .where(SeuilDynamique.id_seuil_dynamique == seuil_id)
            .values(
                valeur_min_calculee=new_min,
                valeur_max_calculee=new_max,
                date_calcul=now,
            )
        )
    """

    __tablename__ = "seuil_dynamique"

    id_seuil_dynamique: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    id_admin: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    # Config field — owned by java-service. Python: read only.
    metrique: Mapped[str] = mapped_column(
        SAEnum(Metrique, name="metrique_seuil_dyn_enum", create_type=False),
        nullable=False,
    )
    # Calculated by python-service (rolling stats).
    valeur_min_calculee: Mapped[Optional[Decimal]] = mapped_column(Numeric(6, 2), nullable=True)
    valeur_max_calculee: Mapped[Optional[Decimal]] = mapped_column(Numeric(6, 2), nullable=True)
    # Config field — owned by java-service. Python: read only.
    marge_configuree: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    # Timestamp of last python recalculation.
    date_calcul: Mapped[Optional[datetime]] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        nullable=False, server_default=text("now()")
    )
    updated_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)
    deleted_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)


class Alerte(Base):
    """
    Module: alerting
    Table SQL: alerte

    HYBRID entity — shared between python-service and java-service.

    Written BY python-service: INSERT only (id_alerte auto via DB, id_mesure,
              metrique, type_alerte, severite, created_at) at ingestion when
              a threshold crossing or AI anomaly is detected.
    Written BY java-service: UPDATE of 'statut' and 'updated_at' / 'deleted_at'
              only (e.g., marking ACTIVE → RESOLUE after an Admin validation).

    IMPORTANT: Python must NEVER modify 'statut' after creation.
    No update_statut() method should be implemented in this module.
    'statut' belongs to java-service's business logic.

    id_mesure is a plain UUID — no ORM relationship to Mesure to avoid
    accidental cascade writes.
    """

    __tablename__ = "alerte"

    id_alerte: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    # Plain UUID reference to mesure.id_mesure — no ORM FK to avoid cascades.
    id_mesure: Mapped[PythonUUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    metrique: Mapped[str] = mapped_column(
        SAEnum(Metrique, name="metrique_alerte_enum", create_type=False),
        nullable=False,
    )
    type_alerte: Mapped[str] = mapped_column(
        SAEnum(TypeAlerte, name="type_alerte_enum", create_type=False),
        nullable=False,
    )
    severite: Mapped[str] = mapped_column(
        SAEnum(Severite, name="severite_enum", create_type=False),
        nullable=False,
    )
    # Owned by java-service after creation. Python sets initial value at INSERT.
    statut: Mapped[str] = mapped_column(
        SAEnum(StatutAlerte, name="statut_alerte_enum", create_type=False),
        nullable=False,
        server_default=text("'ACTIVE'"),
    )
    created_at: Mapped[datetime] = mapped_column(
        nullable=False, server_default=text("now()")
    )
    updated_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)
    deleted_at: Mapped[Optional[datetime]] = mapped_column(nullable=True)
