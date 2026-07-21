from pydantic import BaseModel

class AlertSchema(BaseModel):
    metric: str
    value: float
    threshold: float
    severity: str
