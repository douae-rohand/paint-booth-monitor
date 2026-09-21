from datetime import datetime
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from sqlalchemy.sql.dml import Update

from app.alerting.models import Metrique, SeuilDynamique
from app.plc.ServiceHistorisation import ServiceHistorisation


@pytest.fixture
def service():
    s = ServiceHistorisation(connecteur=MagicMock(), session_factory=MagicMock())
    return s


async def test_recalculer_seuil_dynamique_aucune_config_ne_fait_rien(service, mock_session):
    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = None
    mock_session.execute.side_effect = [res_config]

    await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

    assert mock_session.execute.call_count == 1


async def test_recalculer_seuil_dynamique_aucune_config_log_une_seule_fois(service, mock_session):
    with patch("app.plc.ServiceHistorisation.logger.warning") as mock_warn:
        res1 = MagicMock()
        res1.scalar_one_or_none.return_value = None
        res2 = MagicMock()
        res2.scalar_one_or_none.return_value = None
        mock_session.execute.side_effect = [res1, res2]

        await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)
        await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

        mock_warn.assert_called_once()


async def test_recalculer_seuil_dynamique_moins_de_10_mesures_ignore_recalcul(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique()

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_count = MagicMock()
    res_count.scalar.return_value = 9

    mock_session.execute.side_effect = [res_config, res_count]

    await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

    assert mock_session.execute.call_count == 2


async def test_recalculer_seuil_dynamique_exactement_10_mesures_declenche_recalcul(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique()

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_count = MagicMock()
    res_count.scalar.return_value = 10

    res_avg = MagicMock()
    res_avg.scalar.return_value = Decimal("22.0")

    res_update = MagicMock()

    mock_session.execute.side_effect = [res_config, res_count, res_avg, res_update]

    await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

    assert mock_session.execute.call_count == 4


async def test_recalculer_seuil_dynamique_moyenne_none_ne_fait_rien(
    service, mock_session, make_seuil_dynamique
):
    # Note: Ce cas (nb_mesures >= 10 mais AVG == NULL) est théoriquement inatteignable
    # en usage réel en SQL, mais ce test vérifie la garde défensive du code.
    config = make_seuil_dynamique()

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_count = MagicMock()
    res_count.scalar.return_value = 15

    res_avg = MagicMock()
    res_avg.scalar.return_value = None

    mock_session.execute.side_effect = [res_config, res_count, res_avg]

    await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

    assert mock_session.execute.call_count == 3


async def test_recalculer_seuil_dynamique_calcule_bornes_correctement_depuis_moyenne_et_marge(
    service, mock_session, make_seuil_dynamique
):
    config = make_seuil_dynamique(marge_configuree=Decimal("2.50"))

    res_config = MagicMock()
    res_config.scalar_one_or_none.return_value = config

    res_count = MagicMock()
    res_count.scalar.return_value = 12

    res_avg = MagicMock()
    res_avg.scalar.return_value = Decimal("22.0")

    res_update = MagicMock()

    mock_session.execute.side_effect = [res_config, res_count, res_avg, res_update]

    await service._recalculer_seuil_dynamique(mock_session, 1, Metrique.TEMPERATURE)

    assert mock_session.execute.call_count == 4

    # Inspecter la requête UPDATE passée au 4e appel execute
    update_stmt = mock_session.execute.call_args_list[3][0][0]
    assert isinstance(update_stmt, Update)

    # Inspecter les valeurs passées dans la clause .values() de l'UPDATE
    values = {
        (k.key if hasattr(k, "key") else str(k)): (v.value if hasattr(v, "value") else v)
        for k, v in update_stmt._values.items()
    }
    assert values["valeur_min_calculee"] == pytest.approx(19.50)
    assert values["valeur_max_calculee"] == pytest.approx(24.50)
    assert isinstance(values["date_calcul"], datetime)
