from pydantic import BaseModel
from typing import Optional

class PriceSuggestRequest(BaseModel):
    event_type: str
    duration_hours: float
    city: str
    creator_rate: float

class PriceSuggestResponse(BaseModel):
    suggested_min: float
    suggested_max: float
    market_avg: float
    reasoning: str
