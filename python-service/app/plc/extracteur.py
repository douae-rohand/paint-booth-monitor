"""
Module: plc
Fonction pure d'extraction des mesures depuis un buffer PLC.

Ce module contient uniquement des fonctions de transformation de données,
sans accès réseau ni base de données, pour faciliter les tests unitaires.
"""

from datetime import datetime
from snap7.util import get_real

from app.plc.constants import OFFSETS, PLAUSIBILITE


def extraire_mesures(buffer: bytes, timestamp_cycle: datetime) -> list:
    """
    Extrait les mesures depuis un buffer PLC brut pour tous les points de mesure.

    Cette fonction est pure : elle ne fait que transformer des données,
    sans effet de bord ni accès externe. Elle est facilement testable
    avec des buffers de test sans connexion PLC réelle.

    Args:
        buffer: Buffer brut lu depuis le Data Block PLC (bytes)
        timestamp_cycle: Timestamp unique pour tout le cycle de lecture (datetime)

    Returns:
        list: Liste de dictionnaires, un par (point de mesure × métrique applicable).
              Chaque dictionnaire contient :
                - nom_point_mesure (str): Nom du point de mesure
                - metrique (str): "TEMPERATURE" ou "HUMIDITE"
                - valeur (float): Valeur extraite
                - plausible (bool): True si la valeur est dans la plage de
                                    plausibilité physique de sa métrique
                - timestamp (datetime): Timestamp du cycle de lecture (identique pour toutes les mesures)

    Example:
        >>> buffer = b'\\x00\\x00\\x80\\x3f\\x00\\x00\\x00\\x40'  # 1.0, 2.0
        >>> timestamp = datetime.now()
        >>> extraire_mesures(buffer, timestamp)
        [
            {'nom_point_mesure': "Cabine d'après", 'metrique': 'TEMPERATURE', 'valeur': 1.0, 'plausible': True, 'timestamp': timestamp},
            {'nom_point_mesure': "Cabine d'après", 'metrique': 'HUMIDITE', 'valeur': 2.0, 'plausible': True, 'timestamp': timestamp},
            {'nom_point_mesure': 'Étuve - Zone 1', 'metrique': 'TEMPERATURE', 'valeur': 0.0, 'plausible': True, 'timestamp': timestamp},
            ...
        ]
    """
    mesures = []

    for nom_point_mesure, offsets in OFFSETS.items():
        # Extraire température si présente pour ce point de mesure
        if "temperature" in offsets:
            offset_temp = offsets["temperature"]
            temperature = get_real(buffer, offset_temp)
            temp_min, temp_max = PLAUSIBILITE["temperature"]
            temp_plausible = temp_min <= temperature <= temp_max

            mesures.append({
                "nom_point_mesure": nom_point_mesure,
                "metrique": "TEMPERATURE",
                "valeur": temperature,
                "plausible": temp_plausible,
                "timestamp": timestamp_cycle,
            })

        # Extraire humidité si présente pour ce point de mesure
        if "humidite" in offsets:
            offset_humid = offsets["humidite"]
            humidite = get_real(buffer, offset_humid)
            humid_min, humid_max = PLAUSIBILITE["humidite"]
            humid_plausible = humid_min <= humidite <= humid_max

            mesures.append({
                "nom_point_mesure": nom_point_mesure,
                "metrique": "HUMIDITE",
                "valeur": humidite,
                "plausible": humid_plausible,
                "timestamp": timestamp_cycle,
            })

    return mesures
