from pydantic import BaseModel
from typing import Optional, List

class SearchParseRequest(BaseModel):
    query: str

class SearchParseResponse(BaseModel):
    specializations: List[str] = []
    location_hint: Optional[str] = None
    max_rate_inr: Optional[float] = None
    min_rating: Optional[float] = None
    event_date_hint: Optional[str] = None
    keywords: List[str] = []
