from datetime import datetime
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest

from app.alerting.models import Alerte, Metrique, Severite, SeuilAbsolu, SeuilDynamique, StatutAlerte, TypeAlerte
from app.plc.models import Mesure


@pytest.fixture
def mock_session():
    """
    Fixture simulant une AsyncSession SQLAlchemy.
    - session.execute est un AsyncMock qui retourne un MagicMock configurable.
    - session.add est un MagicMock (méthode synchrone dans AsyncSession).
    - session.flush et session.commit sont des AsyncMock.
    """
    session = AsyncMock()
    session.add = MagicMock()
    session.flush = AsyncMock()
    session.commit = AsyncMock()

    result_mock = MagicMock()
    session.execute = AsyncMock(return_value=result_mock)

    return session


@pytest.fixture
def make_seuil_absolu():
    """Factory fixture pour créer une instance de SeuilAbsolu avec surchages possibles."""
    def _factory(**kwargs):
        defaults = {
            "id_seuil_absolu": uuid4(),
            "id_admin": uuid4(),
            "id_point_mesure": 1,
            "metrique": Metrique.TEMPERATURE.value,
            "valeur_min": Decimal("18.00"),
            "valeur_max": Decimal("25.00"),
            "actif": True,
            "created_at": datetime.now(),
        }
        defaults.update(kwargs)
        return SeuilAbsolu(**defaults)
    return _factory


@pytest.fixture
def make_seuil_dynamique():
    """Factory fixture pour créer une instance de SeuilDynamique avec surchages possibles."""
    def _factory(**kwargs):
        defaults = {
            "id_seuil_dynamique": uuid4(),
            "id_admin": uuid4(),
            "id_point_mesure": 1,
            "metrique": Metrique.TEMPERATURE.value,
            "valeur_min_calculee": Decimal("19.00"),
            "valeur_max_calculee": Decimal("24.00"),
            "marge_configuree": Decimal("2.50"),
            "date_calcul": datetime.now(),
            "created_at": datetime.now(),
        }
        defaults.update(kwargs)
        return SeuilDynamique(**defaults)
    return _factory


@pytest.fixture
def make_alerte():
    """Factory fixture pour créer une instance d'Alerte avec surchages possibles."""
    def _factory(**kwargs):
        defaults = {
            "id_alerte": uuid4(),
            "id_mesure": uuid4(),
            "metrique": Metrique.TEMPERATURE.value,
            "type_alerte": TypeAlerte.SEUIL_ABSOLU.value,
            "severite": Severite.CRITIQUE.value,
            "statut": StatutAlerte.ACTIVE.value,
            "created_at": datetime.now(),
        }
        defaults.update(kwargs)
        return Alerte(**defaults)
    return _factory


@pytest.fixture
def make_mesure():
    """Factory fixture pour créer une instance de Mesure avec surchages possibles."""
    def _factory(**kwargs):
        defaults = {
            "id_mesure": uuid4(),
            "id_point_mesure": 1,
            "metrique": Metrique.TEMPERATURE.value,
            "valeur": Decimal("22.50"),
            "plausible": True,
            "created_at": datetime.now(),
        }
        defaults.update(kwargs)
        return Mesure(**defaults)
    return _factory
