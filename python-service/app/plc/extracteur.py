"""
Module: plc
Fonction pure d'extraction des mesures depuis un buffer PLC.

Ce module contient uniquement des fonctions de transformation de données,
sans accès réseau ni base de données, pour faciliter les tests unitaires.
"""

from snap7.util import get_real

from app.plc.constants import (
    OFFSET_TEMP,
    OFFSET_HUMID,
    TEMP_PLAUSIBLE_MIN,
    TEMP_PLAUSIBLE_MAX,
    HUMID_PLAUSIBLE_MIN,
    HUMID_PLAUSIBLE_MAX,
)


def extraire_mesures(buffer: bytes) -> dict:
    """
    Extrait les mesures de température et d'humidité depuis un buffer PLC brut.
    
    Cette fonction est pure : elle ne fait que transformer des données,
    sans effet de bord ni accès externe. Elle est facilement testable
    avec des buffers de test sans connexion PLC réelle.
    
    Args:
        buffer: Buffer brut lu depuis le Data Block PLC (bytes)
    
    Returns:
        dict: Dictionnaire contenant :
            - temperature (float): Valeur de température extraite
            - humidite (float): Valeur d'humidité extraite
            - plausible (bool): True si les deux valeurs sont dans les plages
                               de plausibilité physique, False sinon
    
    Example:
        >>> buffer = b'\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00'  # 0.0, 0.0
        >>> extraire_mesures(buffer)
        {'temperature': 0.0, 'humidite': 0.0, 'plausible': True}
    """
    # Extraction des floats REAL depuis les offsets spécifiés
    temperature = get_real(buffer, OFFSET_TEMP)
    humidite = get_real(buffer, OFFSET_HUMID)
    
    # Vérification de la plausibilité physique
    temp_plausible = TEMP_PLAUSIBLE_MIN <= temperature <= TEMP_PLAUSIBLE_MAX
    humid_plausible = HUMID_PLAUSIBLE_MIN <= humidite <= HUMID_PLAUSIBLE_MAX
    plausible = temp_plausible and humid_plausible
    
    return {
        "temperature": temperature,
        "humidite": humidite,
        "plausible": plausible,
    }
