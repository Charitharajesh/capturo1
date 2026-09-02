"""
Capturo — Message (Chat) Schemas

Request/response schemas for booking-scoped chat messaging.
"""

from typing import Optional, Literal

from pydantic import BaseModel, ConfigDict, Field
from app.schemas.common import DateTimeUTC


class SendMessageRequest(BaseModel):
    """Send a message within a booking chat thread."""

    booking_id: str
    receiver_id: str
    content: Optional[str] = Field(None, max_length=2000)
    message_type: Literal["text", "image", "file", "system"] = "text"
    media_url: Optional[str] = Field(None, max_length=500)


class MessageResponse(BaseModel):
    """Message record returned in API responses."""

    id: str
    booking_id: str
    sender_id: str
    receiver_id: str
    content: Optional[str] = None
    message_type: str
    media_url: Optional[str] = None
    is_read: bool
    read_at: Optional[DateTimeUTC] = None
    created_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class UnreadCountResponse(BaseModel):
    """Unread message count for notification badges."""

    unread_count: int
