from pydantic import BaseModel

class AnomalyInput(BaseModel):
    temperature: float
    humidity: float

class AnomalyOutput(BaseModel):
    anomaly_score: float
    is_anomaly: bool
