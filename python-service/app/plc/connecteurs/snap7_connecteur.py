import snap7
from app.plc.IConnecteurPLC import IConnecteurPLC


class ConnecteurSnap7(IConnecteurPLC):
    """
    Implémentation concrète de IConnecteurPLC pour la communication via Snap7 avec un automate S7-1200.

    Cette classe ne contient aucune logique métier (extraction, plausibilité).
    Elle se limite à la communication réseau brute avec le PLC.
    """

    def __init__(self, ip: str, rack: int = 0, slot: int = 1, port: int = 102):
        """
        Initialise les paramètres de connexion au PLC.

        Args:
            ip: Adresse IP de l'automate
            rack: Rack (default 0)
            slot: Slot (default 1)
            port: Port TCP (default 102, port standard S7)
        """
        self.ip = ip
        self.rack = rack
        self.slot = slot
        self.port = port
        self.client = snap7.client.Client()

    def connect(self) -> None:
        """
        Établit la liaison TCP vers l'adresse IP de l'automate S7-1200.
        """
        if not self.is_connected():
            self.client.connect(self.ip, self.rack, self.slot, self.port)

    def disconnect(self) -> None:
        """
        Libère la connexion du client Snap7 avec la cible TCP.
        """
        if self.is_connected():
            self.client.disconnect()

    def is_connected(self) -> bool:
        """
        Vérifie si la connexion est active.
        """
        return self.client.get_connected()

    def read_db(self, db_number: int, start: int, size: int) -> bytes:
        """
        Lit un bloc de données brut depuis un Data Block du PLC.

        Args:
            db_number: Numéro du Data Block
            start: Offset de départ en octets
            size: Nombre d'octets à lire

        Returns:
            Buffer brut (bytes) sans aucune transformation métier
        """
        self.connect()
        return self.client.db_read(db_number, start, size)
