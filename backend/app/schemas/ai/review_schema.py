from pydantic import BaseModel
from typing import Optional, List

class ReviewInput(BaseModel):
    comment: str
    rating: int

class ReviewSummariseRequest(BaseModel):
    creator_id: str
    creator_name: str
    reviews: List[ReviewInput]

class ReviewSummariseResponse(BaseModel):
    summary: str
    highlights: List[str]
    watch_out: Optional[str] = None
    sentiment_score: float
