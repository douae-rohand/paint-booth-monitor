from datetime import datetime
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4

import pytest

from app.alerting.models import Metrique, Severite, TypeAlerte
from app.plc.ServiceHistorisation import ServiceHistorisation


@pytest.fixture
def service():
    s = ServiceHistorisation(connecteur=MagicMock(), session_factory=MagicMock())
    s._notifier_nouvelle_alerte = AsyncMock()
    s._notifier_alerte_resolue = AsyncMock()
    return s


async def test_verifier_seuil_dynamique_aucune_config_ne_cree_pas_alerte(service, mock_session):
    result_config = MagicMock()
    result_config.scalar_one_or_none.return_value = None
    mock_session.execute.side_effect = [result_config]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 25.0)

    mock_session.add.assert_not_called()
    service._notifier_nouvelle_alerte.assert_not_called()
    assert mock_session.execute.call_count == 1


async def test_verifier_seuil_dynamique_aucune_config_log_une_seule_fois(service, mock_session):
    with patch("app.plc.ServiceHistorisation.logger.warning") as mock_warn:
        res1 = MagicMock()
        res1.scalar_one_or_none.return_value = None
        res2 = MagicMock()
        res2.scalar_one_or_none.return_value = None
        mock_session.execute.side_effect = [res1, res2]

        await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 25.0)
        await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 25.0)

        mock_warn.assert_called_once()


async def test_verifier_seuil_dynamique_hors_bornes_sans_alerte_active_cree_alerte_moyenne(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = None

    mock_session.execute.side_effect = [res_config, res_alerte]

    id_mesure = uuid4()
    await service._verifier_seuil_dynamique(mock_session, id_mesure, 1, Metrique.TEMPERATURE, 30.0)

    mock_session.add.assert_called_once()
    added_alerte = mock_session.add.call_args[0][0]
    assert added_alerte.severite == Severite.MOYENNE
    assert added_alerte.type_alerte == TypeAlerte.SEUIL_DYNAMIQUE
    mock_session.flush.assert_called_once()
    service._notifier_nouvelle_alerte.assert_called_once()


async def test_verifier_seuil_dynamique_hors_bornes_avec_alerte_active_ne_cree_pas_doublon(
    service, mock_session, make_seuil_dynamique, make_alerte
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )
    alerte_active = make_alerte(statut="ACTIVE", type_alerte=TypeAlerte.SEUIL_DYNAMIQUE.value)

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = alerte_active

    mock_session.execute.side_effect = [res_config, res_alerte]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 30.0)

    mock_session.add.assert_not_called()
    service._notifier_nouvelle_alerte.assert_not_called()
    service._notifier_alerte_resolue.assert_not_called()


async def test_verifier_seuil_dynamique_dans_bornes_avec_alerte_active_resout_alerte(
    service, mock_session, make_seuil_dynamique, make_alerte
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )
    initial_updated_at = datetime(2026, 1, 1, 0, 0, 0)
    alerte_active = make_alerte(
        statut="ACTIVE",
        type_alerte=TypeAlerte.SEUIL_DYNAMIQUE.value,
        updated_at=initial_updated_at,
    )

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = alerte_active

    mock_session.execute.side_effect = [res_config, res_alerte]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 21.0)

    assert alerte_active.statut == "RESOLUE"
    assert alerte_active.updated_at != initial_updated_at
    mock_session.add.assert_called_once_with(alerte_active)
    service._notifier_alerte_resolue.assert_called_once_with(mock_session, alerte_active.id_alerte)


async def test_verifier_seuil_dynamique_dans_bornes_sans_alerte_active_ne_fait_rien(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = None

    mock_session.execute.side_effect = [res_config, res_alerte]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 21.0)

    mock_session.add.assert_not_called()
    service._notifier_nouvelle_alerte.assert_not_called()
    service._notifier_alerte_resolue.assert_not_called()


async def test_verifier_seuil_dynamique_valeur_exactement_sur_borne_min_ne_declenche_pas_alerte(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = None

    mock_session.execute.side_effect = [res_config, res_alerte]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 19.0)

    mock_session.add.assert_not_called()


async def test_verifier_seuil_dynamique_valeur_exactement_sur_borne_max_ne_declenche_pas_alerte(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique(
        valeur_min_calculee=Decimal("19.00"),
        valeur_max_calculee=Decimal("24.00"),
    )

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_alerte = MagicMock()
    res_alerte.scalar_one_or_none.return_value = None

    mock_session.execute.side_effect = [res_config, res_alerte]

    await service._verifier_seuil_dynamique(mock_session, uuid4(), 1, Metrique.TEMPERATURE, 24.0)

    mock_session.add.assert_not_called()
