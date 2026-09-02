"""
Capturo — Review Schemas

Request/response schemas for the review and rating system.
"""

from typing import Optional

from pydantic import BaseModel, Field, field_validator, ConfigDict
from app.schemas.common import DateTimeUTC


class CreateReviewRequest(BaseModel):
    """Submit a review for a completed booking."""

    booking_id: str
    rating: int = Field(..., ge=1, le=5)
    comment: Optional[str] = Field(None, max_length=1000)

    @field_validator("rating")
    @classmethod
    def rating_range(cls, v: int) -> int:
        if v < 1 or v > 5:
            raise ValueError("Rating must be between 1 and 5")
        return v


class UpdateReviewRequest(BaseModel):
    """Edit a review (allowed within 24 hours of submission)."""

    rating: Optional[int] = Field(None, ge=1, le=5)
    comment: Optional[str] = Field(None, max_length=1000)


class ReviewResponse(BaseModel):
    """Review detail returned in API responses."""

    id: str
    booking_id: str
    reviewer_id: str
    creator_id: str
    rating: int
    comment: Optional[str] = None
    is_verified: bool
    created_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)
