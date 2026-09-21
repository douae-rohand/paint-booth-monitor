import struct
from datetime import datetime
import pytest

from app.plc.extracteur import extraire_mesures
from app.plc.constants import READ_SIZE

FIXED_TIMESTAMP = datetime(2026, 9, 20, 15, 0, 0)


def creer_buffer_vide() -> bytearray:
    """Crée un buffer de taille READ_SIZE rempli de 0."""
    return bytearray(READ_SIZE)


def ecrire_real(buffer: bytearray, offset: int, valeur: float) -> None:
    """Écrit un float IEEE 754 32-bit big-endian aux octets [offset:offset+4]."""
    buffer[offset : offset + 4] = struct.pack(">f", valeur)


def chercher_mesure(mesures: list, nom_point: str, metrique: str) -> dict:
    """Retrouve une mesure par son nom de point et sa métrique."""
    for m in mesures:
        if m["nom_point_mesure"] == nom_point and m["metrique"] == metrique:
            return m
    return None


def test_extraire_mesures_valeur_temperature_dans_plage_plausible_true():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 0, 25.0)  # Cabine d'après - temperature

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesure = chercher_mesure(mesures, "Cabine d'après", "TEMPERATURE")

    assert mesure is not None
    assert mesure["nom_point_mesure"] == "Cabine d'après"
    assert mesure["metrique"] == "TEMPERATURE"
    assert mesure["valeur"] == pytest.approx(25.0)
    assert mesure["plausible"] is True
    assert mesure["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_valeur_temperature_hors_plage_plausible_false():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 0, 300.0)  # Cabine d'après - temperature hors (-20, 250)

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesure = chercher_mesure(mesures, "Cabine d'après", "TEMPERATURE")

    assert mesure is not None
    assert mesure["nom_point_mesure"] == "Cabine d'après"
    assert mesure["metrique"] == "TEMPERATURE"
    assert mesure["valeur"] == pytest.approx(300.0)
    assert mesure["plausible"] is False
    assert mesure["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_valeur_humidite_dans_plage_plausible_true():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 4, 45.0)  # Cabine d'après - humidite

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesure = chercher_mesure(mesures, "Cabine d'après", "HUMIDITE")

    assert mesure is not None
    assert mesure["nom_point_mesure"] == "Cabine d'après"
    assert mesure["metrique"] == "HUMIDITE"
    assert mesure["valeur"] == pytest.approx(45.0)
    assert mesure["plausible"] is True
    assert mesure["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_valeur_humidite_hors_plage_plausible_false():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 4, 150.0)  # Cabine d'après - humidite hors (0, 100)

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesure = chercher_mesure(mesures, "Cabine d'après", "HUMIDITE")

    assert mesure is not None
    assert mesure["nom_point_mesure"] == "Cabine d'après"
    assert mesure["metrique"] == "HUMIDITE"
    assert mesure["valeur"] == pytest.approx(150.0)
    assert mesure["plausible"] is False
    assert mesure["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_meme_timestamp_cycle_pour_toutes_les_mesures_du_meme_appel():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 0, 25.0)   # Cabine d'après - temperature
    ecrire_real(buffer, 8, 180.0)  # Étuve - Zone 1 - temperature

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)

    assert len(mesures) >= 2
    for mesure in mesures:
        assert mesure["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_point_avec_temperature_et_humidite_produit_deux_entrees():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 0, 25.0)  # Cabine d'après - temperature
    ecrire_real(buffer, 4, 45.0)  # Cabine d'après - humidite

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesures_cabine = [m for m in mesures if m["nom_point_mesure"] == "Cabine d'après"]

    assert len(mesures_cabine) == 2

    m_temp = chercher_mesure(mesures_cabine, "Cabine d'après", "TEMPERATURE")
    assert m_temp is not None
    assert m_temp["valeur"] == pytest.approx(25.0)
    assert m_temp["plausible"] is True
    assert m_temp["timestamp"] == FIXED_TIMESTAMP

    m_hum = chercher_mesure(mesures_cabine, "Cabine d'après", "HUMIDITE")
    assert m_hum is not None
    assert m_hum["valeur"] == pytest.approx(45.0)
    assert m_hum["plausible"] is True
    assert m_hum["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_point_avec_une_seule_metrique_produit_une_seule_entree():
    buffer = creer_buffer_vide()
    ecrire_real(buffer, 8, 180.0)  # Étuve - Zone 1 - temperature

    mesures = extraire_mesures(bytes(buffer), FIXED_TIMESTAMP)
    mesures_etuve1 = [m for m in mesures if m["nom_point_mesure"] == "Étuve - Zone 1"]

    assert len(mesures_etuve1) == 1
    m_etuve = mesures_etuve1[0]
    assert m_etuve["nom_point_mesure"] == "Étuve - Zone 1"
    assert m_etuve["metrique"] == "TEMPERATURE"
    assert m_etuve["valeur"] == pytest.approx(180.0)
    assert m_etuve["plausible"] is True
    assert m_etuve["timestamp"] == FIXED_TIMESTAMP


def test_extraire_mesures_valeur_exactement_sur_borne_plausibilite_est_plausible():
    # Borne minimale (-20.0)
    buffer_min = creer_buffer_vide()
    ecrire_real(buffer_min, 0, -20.0)
    mesures_min = extraire_mesures(bytes(buffer_min), FIXED_TIMESTAMP)
    m_min = chercher_mesure(mesures_min, "Cabine d'après", "TEMPERATURE")
    assert m_min is not None
    assert m_min["nom_point_mesure"] == "Cabine d'après"
    assert m_min["metrique"] == "TEMPERATURE"
    assert m_min["valeur"] == pytest.approx(-20.0)
    assert m_min["plausible"] is True
    assert m_min["timestamp"] == FIXED_TIMESTAMP

    # Borne maximale (250.0)
    buffer_max = creer_buffer_vide()
    ecrire_real(buffer_max, 0, 250.0)
    mesures_max = extraire_mesures(bytes(buffer_max), FIXED_TIMESTAMP)
    m_max = chercher_mesure(mesures_max, "Cabine d'après", "TEMPERATURE")
    assert m_max is not None
    assert m_max["nom_point_mesure"] == "Cabine d'après"
    assert m_max["metrique"] == "TEMPERATURE"
    assert m_max["valeur"] == pytest.approx(250.0)
    assert m_max["plausible"] is True
    assert m_max["timestamp"] == FIXED_TIMESTAMP
