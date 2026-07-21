import abc
from app.plc.schemas import MesureCapteur

class IConnecteurPLC(abc.ABC):
    """
    Interface abstraite définissant le contrat de communication avec un automate PLC.
    """

    @abc.abstractmethod
    def connect(self) -> None:
        """
        Établit la connexion physique ou logique avec l'automate.
        """
        pass

    @abc.abstractmethod
    def read_mesure(self) -> MesureCapteur:
        """
        Lit les données du PLC et retourne une mesure typée.
        """
        pass

    @abc.abstractmethod
    def disconnect(self) -> None:
        """
        Ferme et libère la connexion avec l'automate.
        """
        pass
