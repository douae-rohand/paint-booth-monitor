"""
Tests manuels pour le service d'historisation PLC.

Ce fichier contient un test ponctuel de connexion PLC utilisant la nouvelle architecture
avec ConnecteurSnap7 (implémente IConnecteurPLC) et l'extracteur de données.

Usage:
    python tests/plc/test_service_historisation.py

Ce test vérifie uniquement que la connexion Snap7 fonctionne et que l'extraction
des données depuis le buffer PLC est correcte. Il n'utilise pas la base de données
ni les tâches asyncio de polling.
"""

from app.plc.connecteurs.snap7_connecteur import ConnecteurSnap7
from app.plc.extracteur import extraire_mesures
from app.plc.constants import DB_NUMBER, START_OFFSET, READ_SIZE


if __name__ == "__main__":
    # Paramètres de test par défaut
    automate_ip = "127.0.0.1"  # Utiliser 127.0.0.1 pour le simulateur local
    rack = 0
    slot = 0

    print(f"Test de connexion PLC : {automate_ip}, rack={rack}, slot={slot}")

    connecteur = ConnecteurSnap7(ip=automate_ip, rack=rack, slot=slot)

    try:
        # Connexion au PLC
        connecteur.connect()
        print("✓ Connexion établie")

        # Lecture du buffer depuis le PLC via l'interface
        buffer = connecteur.read_db(DB_NUMBER, START_OFFSET, READ_SIZE)
        print(f"✓ Buffer lu ({len(buffer)} octets)")

        # Extraction des mesures via la fonction pure (retourne une liste)
        mesures = extraire_mesures(buffer)

        print(f"\n{len(mesures)} mesures extraites :")
        for mesure in mesures:
            print(f"  {mesure['nom_point_mesure']} - {mesure['metrique']} : "
                  f"{mesure['valeur']:.2f} (plausible={mesure['plausible']})")

    except Exception as e:
        print(f"\n✗ Erreur lors du test : {e}")
        import traceback
        traceback.print_exc()
    finally:
        connecteur.disconnect()
        print("✓ Connexion fermée")
