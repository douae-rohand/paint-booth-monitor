from pydantic import BaseModel
from datetime import datetime
from app.plc.models import Metrique

class MesureSchema(BaseModel):
    """
    Modèle Pydantic pour une mesure avec métrique validée.
    """
    metrique: Metrique
    valeur: float
    plausible: bool
    timestamp: datetime
