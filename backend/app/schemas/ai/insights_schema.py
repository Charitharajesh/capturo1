from pydantic import BaseModel
from typing import List, Dict, Any

class InsightsRequest(BaseModel):
    stats: Dict[str, Any]

class InsightsResponse(BaseModel):
    trend: str
    trend_pct: float
    forecast_next_month_inr: float
    top_insight: str
    recommendations: List[str]
