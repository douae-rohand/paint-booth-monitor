from pydantic import BaseModel
from datetime import datetime

class MesureCapteur(BaseModel):
    """
    Modèle Pydantic représentant une mesure de température et d'humidité à un instant donné.
    """
    temperature: float
    humidite: float
    timestamp: datetime
