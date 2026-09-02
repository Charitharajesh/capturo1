# app/schemas/ai/matching_schema.py

from pydantic import BaseModel
from typing import Optional, List, Dict, Any

class BookingContext(BaseModel):
    event_type: str
    location: str
    budget_inr: float
    event_date: str
    notes: Optional[str] = None

class CreatorInput(BaseModel):
    id: str
    specializations: List[str]
    hourly_rate: float
    avg_rating: float
    distance_km: float

class MatchRequest(BaseModel):
    booking_context: BookingContext
    creators: List[CreatorInput]

class RankedCreator(BaseModel):
    id: str
    ai_score: int           # 0-100
    ai_reason: str          # "Specializes in weddings, within budget"

class MatchResponse(BaseModel):
    creators: List[Dict[str, Any]]    # Full creator objects with ai_score + ai_reason injected
    ai_powered: bool = True
