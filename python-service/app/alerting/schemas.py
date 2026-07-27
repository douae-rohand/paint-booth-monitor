from pydantic import BaseModel
from app.plc.models import Metrique

class AlertSchema(BaseModel):
    metric: Metrique
    value: float
    threshold: float
    severity: str
