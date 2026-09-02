"""
Capturo — Booking Schemas

Request/response schemas for the entire booking lifecycle:
create → confirm → complete → cancel → dispute → invoice.

All money fields use Decimal (Rule Set 5). All datetime fields use
AwareDatetime for ISO-8601 UTC output (Rule Set 4).
"""

from typing import Optional, Literal, List
from decimal import Decimal
from datetime import date, time

from pydantic import BaseModel, Field, field_validator, ConfigDict, model_validator
from app.schemas.common import DateTimeUTC
from app.schemas.user import UserResponse
from app.schemas.creator import CreatorBriefResponse


# ── Request Schemas ──────────────────────────────────────────────────

class CreateBookingRequest(BaseModel):
    """Create a new booking with full validation (Rule Set 5)."""

    creator_id: str = Field(..., min_length=36, max_length=36)
    event_type: Literal["wedding", "birthday", "corporate", "graduation", "party", "other"]
    location: str = Field(..., min_length=5, max_length=500)
    event_date: date
    start_time: time
    duration_hours: Decimal = Field(..., ge=Decimal("1.0"), le=Decimal("12.0"))
    special_notes: Optional[str] = Field(None, max_length=1000)

    @field_validator("event_date")
    @classmethod
    def date_must_be_future(cls, v: date) -> date:
        if v <= date.today():
            raise ValueError("Event date must be in the future")
        return v

    @field_validator("duration_hours")
    @classmethod
    def duration_step(cls, v: Decimal) -> Decimal:
        """Duration must be in 0.5 hour increments."""
        if v * 2 != int(v * 2):
            raise ValueError("Duration must be in 0.5 hour increments")
        return v


class UpdateBookingRequest(BaseModel):
    """Update booking details before confirmation."""

    location: Optional[str] = Field(None, min_length=5, max_length=500)
    special_notes: Optional[str] = Field(None, max_length=1000)


class ConfirmBookingPaymentRequest(BaseModel):
    """Razorpay payment confirmation from client."""

    payment_id: str
    payment_signature: str


class CancelBookingRequest(BaseModel):
    """Booking cancellation with mandatory reason."""

    reason: str = Field(..., min_length=5, max_length=500)


class CompleteBookingRequest(BaseModel):
    """Creator marks booking as completed."""

    completion_notes: Optional[str] = Field(None, max_length=1000)


class DisputeBookingRequest(BaseModel):
    """Raise a dispute on a booking."""

    reason: str = Field(..., min_length=10, max_length=1000)
    evidence_urls: Optional[List[str]] = Field(None)


# ── Response Schemas ─────────────────────────────────────────────────

class BookingBriefResponse(BaseModel):
    """Compact booking representation for list views."""

    id: str
    event_type: str
    location: str
    event_date: date
    start_time: time
    duration_hours: Decimal
    total_amount: Decimal
    status: str
    created_at: DateTimeUTC
    creator: Optional[CreatorBriefResponse] = None
    attendee: Optional[UserResponse] = None
    model_config = ConfigDict(from_attributes=True)

    @model_validator(mode="before")
    @classmethod
    def resolve_creator_profile(cls, data):
        if isinstance(data, dict):
            return data
        
        # If it's a database model object (SQLAlchemy model)
        if not hasattr(data, "creator_id"):
            return data

        # Extract user
        user = data.creator
        creator_data = None
        if user and user.creator_profile:
            profile = user.creator_profile
            creator_data = {
                "id": profile.id,
                "user_id": user.id,
                "full_name": user.full_name,
                "email": user.email,
                "phone": user.phone,
                "profile_pic_url": user.profile_pic_url,
                "hourly_rate": profile.hourly_rate,
                "avg_rating": profile.avg_rating,
                "total_reviews": profile.total_reviews,
                "availability_status": profile.availability_status,
            }

        # Extract attendee
        attendee_user = data.attendee
        attendee_data = None
        if attendee_user:
            attendee_data = {
                "id": attendee_user.id,
                "full_name": attendee_user.full_name,
                "email": attendee_user.email,
                "phone": attendee_user.phone,
                "profile_pic_url": attendee_user.profile_pic_url,
                "role": attendee_user.role,
                "is_active": attendee_user.is_active,
                "is_verified": attendee_user.is_verified,
                "last_login_at": attendee_user.last_login_at,
                "created_at": attendee_user.created_at,
                "updated_at": attendee_user.updated_at,
            }

        # Return a dictionary of attributes
        res = {
            "id": data.id,
            "event_type": data.event_type,
            "location": data.location,
            "event_date": data.event_date,
            "start_time": data.start_time,
            "duration_hours": data.duration_hours,
            "total_amount": data.total_amount,
            "status": data.status,
            "created_at": data.created_at,
            "creator": creator_data,
            "attendee": attendee_data,
        }

        # If it's a subclass (like BookingResponse), also copy its specific fields!
        if hasattr(data, "attendee_id"):
            res["attendee_id"] = data.attendee_id
            res["creator_id"] = data.creator_id
            res["special_notes"] = data.special_notes
            res["invoice_url"] = data.invoice_url
            res["cancelled_by"] = data.cancelled_by
            res["cancellation_reason"] = data.cancellation_reason
            res["updated_at"] = data.updated_at

        return res


class BookingResponse(BookingBriefResponse):
    """Full booking detail including all fields."""

    attendee_id: str
    creator_id: str
    special_notes: Optional[str] = None
    invoice_url: Optional[str] = None
    cancelled_by: Optional[str] = None
    cancellation_reason: Optional[str] = None
    updated_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class BookingCreatedResponse(BaseModel):
    """Response after booking creation with Razorpay order details."""

    booking_id: str
    status: str
    payment_order_id: str
    payment_key_id: str
    total_amount: Decimal


class CancelBookingResponse(BaseModel):
    """Response after booking cancellation with refund info."""

    status: str
    refund_amount: Decimal
    refund_eta_days: int = 5


class InvoiceResponse(BaseModel):
    """Booking invoice URL and generation timestamp."""

    invoice_url: str
    generated_at: DateTimeUTC
