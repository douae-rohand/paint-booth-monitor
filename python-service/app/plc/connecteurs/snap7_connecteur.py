import snap7
from snap7.util import get_real
from datetime import datetime
from app.plc.IConnecteurPLC import IConnecteurPLC
from app.plc.schemas import MesureCapteur

# Paramètres configurables du Data Block automate
DB_NUMBER = 1
OFFSET_TEMP = 0
OFFSET_HUMID = 4
READ_SIZE = 8

class ConnecteurSnap7(IConnecteurPLC):
    """
    Implémentation concrète de IConnecteurPLC pour la communication via Snap7 avec un automate S7-1200.
    """

    def __init__(self, ip: str, rack: int = 0, slot: int = 1):
        """
        Initialise les variables d'adresse physique de la cible IP automate.
        """
        self.ip = ip
        self.rack = rack
        self.slot = slot
        self.client = snap7.client.Client()

    def connect(self) -> None:
        """
        Établit la liaison TCP vers l'adresse IP de l'automate S7-1200.
        """
        if not self.client.get_connected():
            self.client.connect(self.ip, self.rack, self.slot)

    def read_mesure(self) -> MesureCapteur:
        """
        Récupère les bytes bruts d'un DB S7, extrait et convertit les températures
        et humidité codées en REAL/Float.
        """
        self.connect()
        raw_data = self.client.db_read(DB_NUMBER, OFFSET_TEMP, READ_SIZE)
        
        # Décoder les Float 32 bits issus des offsets spécifiés
        temp = get_real(raw_data, 0)
        humid = get_real(raw_data, OFFSET_HUMID - OFFSET_TEMP)

        return MesureCapteur(
            temperature=temp,
            humidite=humid,
            timestamp=datetime.now()
        )

    def disconnect(self) -> None:
        """
        Libère la connexion du client Snap7 avec la cible TCP.
        """
        if self.client.get_connected():
            self.client.disconnect()
