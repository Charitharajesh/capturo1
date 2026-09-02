"""
Capturo — Notification Schemas

Request/response schemas for in-app and push notifications.
All created_at fields use AwareDatetime for ISO-8601 UTC output —
the Android client uses these for relative time calculations.
"""

from typing import Optional

from pydantic import BaseModel, ConfigDict
from app.schemas.common import DateTimeUTC


class NotificationResponse(BaseModel):
    """In-app notification record."""

    id: str
    user_id: str
    title: str
    body: str
    notification_type: str
    reference_id: Optional[str] = None
    reference_type: Optional[str] = None
    is_read: bool
    read_at: Optional[DateTimeUTC] = None
    created_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class BroadcastNotificationRequest(BaseModel):
    """Admin request to send a notification to a user segment."""

    title: str
    body: str
    target: str  # "all_users", "all_creators", "all_attendees"


class UpdatedCountResponse(BaseModel):
    """Response for bulk update operations."""

    updated_count: int
