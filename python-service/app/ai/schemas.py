from pydantic import BaseModel
from app.ai.models import ModeleIA
from app.plc.models import Metrique

class AnomalyInput(BaseModel):
    """
    Modèle Pydantic pour l'entrée d'analyse d'anomalie avec métrique validée.
    """
    metrique: Metrique
    valeur: float

class AnomalyOutput(BaseModel):
    """
    Modèle Pydantic pour la sortie d'analyse d'anomalie.
    """
    anomaly_score: float
    is_anomaly: bool

class PredictionIASchema(BaseModel):
    """
    Modèle Pydantic pour une prédiction IA avec modèle validé.
    """
    modele_utilise: ModeleIA
    valeur_predite: float | None
    valeur_reelle: float | None
    est_anomalie: bool | None
