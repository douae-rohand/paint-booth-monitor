import abc


class IConnecteurPLC(abc.ABC):
    """
    Interface abstraite définissant le contrat minimal de communication avec un automate PLC.

    Cette interface ne contient aucune connaissance métier (température, humidité, DB_NUMBER).
    Elle se limite aux opérations de connexion et de lecture brute de données.
    """

    @abc.abstractmethod
    def connect(self) -> None:
        """
        Établit la connexion physique ou logique avec l'automate.
        """
        pass

    @abc.abstractmethod
    def disconnect(self) -> None:
        """
        Ferme et libère la connexion avec l'automate.
        """
        pass

    @abc.abstractmethod
    def is_connected(self) -> bool:
        """
        Vérifie si la connexion est active.
        """
        pass

    @abc.abstractmethod
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
        pass
