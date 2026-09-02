"""
Capturo — Creator Schemas

Request/response schemas for creator profile, nearby search,
availability checks, and dashboard stats.
"""

from typing import List, Optional
from decimal import Decimal

from pydantic import BaseModel, Field, ConfigDict
from app.schemas.common import DateTimeUTC
from app.schemas.user import UserResponse


# ── Request Schemas ──────────────────────────────────────────────────

class CreatorProfileBase(BaseModel):
    """Shared creator profile fields."""

    specializations: List[str] = Field(default_factory=list)
    hourly_rate: Decimal = Field(default=Decimal("999.00"), ge=Decimal("0.00"))
    minimum_hours: int = Field(default=2, ge=1)
    bio: Optional[str] = Field(None, max_length=1000)
    years_experience: Optional[int] = Field(None, ge=0)
    equipment: Optional[List[str]] = Field(None)
    availability_status: str = Field("available", pattern="^(available|busy|offline)$")
    latitude: Optional[Decimal] = Field(None, ge=Decimal("-90.0"), le=Decimal("90.0"))
    longitude: Optional[Decimal] = Field(None, ge=Decimal("-180.0"), le=Decimal("180.0"))
    service_radius_km: int = Field(default=10, ge=1)


class CreateCreatorRequest(CreatorProfileBase):
    """Create a new creator profile."""
    pass


class UpdateCreatorRequest(BaseModel):
    """Partial update for creator profile — all fields optional."""

    specializations: Optional[List[str]] = Field(None)
    hourly_rate: Optional[Decimal] = Field(None, ge=Decimal("0.00"))
    minimum_hours: Optional[int] = Field(None, ge=1)
    bio: Optional[str] = Field(None, max_length=1000)
    years_experience: Optional[int] = Field(None, ge=0)
    equipment: Optional[List[str]] = Field(None)
    availability_status: Optional[str] = Field(None, pattern="^(available|busy|offline)$")
    service_radius_km: Optional[int] = Field(None, ge=1)
    latitude: Optional[Decimal] = Field(None, ge=Decimal("-90.0"), le=Decimal("90.0"))
    longitude: Optional[Decimal] = Field(None, ge=Decimal("-180.0"), le=Decimal("180.0"))


# ── Response Schemas ─────────────────────────────────────────────────

class CreatorProfileResponse(CreatorProfileBase):
    """Full creator profile response with computed stats."""

    id: str
    user_id: str
    avg_rating: Decimal
    total_reviews: int
    total_bookings: int
    on_time_rate: Decimal
    is_featured: bool
    followers_count: int = 0
    created_at: DateTimeUTC
    updated_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class CreatorBriefResponse(BaseModel):
    """Compact creator info for list views and nearby search."""

    id: str
    user_id: str
    full_name: str
    email: str
    phone: Optional[str] = None
    profile_pic_url: Optional[str] = None
    hourly_rate: Decimal
    avg_rating: Decimal
    total_reviews: int
    availability_status: str
    latitude: Optional[Decimal] = None
    longitude: Optional[Decimal] = None
    model_config = ConfigDict(from_attributes=True)


class CreatorPublicResponse(CreatorProfileResponse):
    """Public creator detail with nested user info."""

    user: Optional[UserResponse] = None
    followers_count: int = 0
    is_following: bool = False
    model_config = ConfigDict(from_attributes=True)


class CreatorDistanceResponse(BaseModel):
    """Creator result with calculated distance for nearby search."""

    creator: CreatorBriefResponse
    distance_km: Decimal
    model_config = ConfigDict(from_attributes=True)


class AvailabilityCheckResponse(BaseModel):
    """Availability check result for a specific date/time slot."""

    is_available: bool
    conflicting_bookings: List[str] = Field(default_factory=list)


class CreatorStatsResponse(BaseModel):
    """Creator dashboard statistics."""

    earnings_this_month: Decimal
    bookings: int
    rating: Decimal
    earnings_last_month: Decimal = Decimal("0.00")
    revenue_growth_percentage: float = 0.0
    model_config = ConfigDict(from_attributes=True)
