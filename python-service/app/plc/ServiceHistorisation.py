import snap7
from snap7.util import get_real

# Paramètres configurables de lecture du Data Block (DB) de l'automate
DB_NUMBER = 1
START_OFFSET = 0
READ_SIZE = 8
OFFSET_TEMP = 0
OFFSET_HUMID = 4

# TODO refactor (étape 2): cette classe sera renommée ConnecteurSnap7, déplacée dans plc/connecteurs/snap7_connecteur.py, et devra hériter de IConnecteurPLC (plc/interfaces.py)
class ServiceHistorisation:
    """
    Classe simple pour gérer la connexion et la lecture directe des mesures de
    température et d'humidité à partir d'un automate Siemens S7-1200 via Snap7.
    """

    def __init__(self, ip: str, rack: int = 0, slot: int = 1):
        """
        Initialise le client Snap7 avec les paramètres réseau de l'automate.
        """
        self.ip = ip
        self.rack = rack
        self.slot = slot
        self.client = snap7.client.Client()

    def connect(self) -> None:
        """
        Établit la connexion TCP/IP avec l'automate.
        """
        try:
            print(f"Tentative de connexion à l'automate ({self.ip}, rack={self.rack}, slot={self.slot})...")
            self.client.connect(self.ip, self.rack, self.slot)
            if self.client.get_connected():
                print("Connexion établie avec succès.")
            else:
                print("Échec de connexion : automate injoignable.")
        except Exception as e:
            print(f"Erreur lors de la tentative de connexion : {e}")
            raise e

    # TODO refactor (étape 2): le type de retour deviendra MesureCapteur (Pydantic, plc/schemas.py) au lieu d'un tuple
    def read_mesure(self) -> tuple[float, float]:
        """
        Lit le bloc de données spécifié sur l'automate et extrait les valeurs.
        """
        if not self.client.get_connected():
            self.connect()

        # Lecture brute des bytes du DB
        raw_data = self.client.db_read(DB_NUMBER, START_OFFSET, READ_SIZE)

        # Décodage des floats (REAL de 4 octets)
        temperature = get_real(raw_data, OFFSET_TEMP)
        humidite = get_real(raw_data, OFFSET_HUMID)

        # TODO refactor (étape 2): construire et retourner un objet MesureCapteur avec un champ timestamp en plus
        return temperature, humidite

    def disconnect(self) -> None:
        """
        Ferme proprement la connexion active avec l'automate.
        """
        if self.client.get_connected():
            print("Fermeture de la connexion PLC...")
            self.client.disconnect()
            print("Connexion fermée.")


# TODO refactor (étape 2): ce bloc de test disparaîtra d'ici — une nouvelle classe ServiceHistorisation (orchestration) sera créée séparément, qui recevra un IConnecteurPLC en injection de dépendance au lieu de contenir directement la logique Snap7
if __name__ == "__main__":
    # Paramètres de test par défaut
    automate_ip = "192.168.0.1"
    
    service = ServiceHistorisation(ip=automate_ip, rack=0, slot=1)
    
    try:
        service.connect()
        temp, hum = service.read_mesure()
        print(f"\nMesure récupérée : Température = {temp:.2f}°C, Humidité = {hum:.2f}%\n")
    except Exception as e:
        print(f"\nUne erreur est survenue lors de l'exécution du test : {e}\n")
    finally:
        service.disconnect()
