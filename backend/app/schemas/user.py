"""
Capturo — User Schemas

Request/response schemas for user profile operations.
"""

from typing import Optional

from pydantic import BaseModel, EmailStr, Field, ConfigDict
from app.schemas.common import DateTimeUTC


class UserBase(BaseModel):
    """Shared user fields for creation."""

    full_name: str = Field(..., min_length=2, max_length=150)
    email: EmailStr
    phone: Optional[str] = Field(None, max_length=20)
    profile_pic_url: Optional[str] = Field(None, max_length=500)


class UpdateUserRequest(BaseModel):
    """Partial update for authenticated user's own profile."""

    full_name: Optional[str] = Field(None, min_length=2, max_length=150)
    phone: Optional[str] = Field(None, max_length=20)
    profile_pic_url: Optional[str] = Field(None, max_length=500)


class UserResponse(BaseModel):
    """User profile returned in API responses. Never includes hashed_password."""

    id: str
    full_name: str
    email: str
    phone: Optional[str] = None
    profile_pic_url: Optional[str] = None
    role: str
    is_active: bool
    is_verified: bool
    last_login_at: Optional[DateTimeUTC] = None
    created_at: DateTimeUTC
    updated_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class UserPublicResponse(BaseModel):
    """Public user profile — limited fields for non-owner views."""

    id: str
    full_name: str
    profile_pic_url: Optional[str] = None
    role: str
    model_config = ConfigDict(from_attributes=True)


class UserStatusUpdateRequest(BaseModel):
    """Admin request to activate/deactivate a user."""

    is_active: bool


class FCMTokenRequest(BaseModel):
    """Register or update device FCM token for push notifications."""

    fcm_token: str = Field(..., min_length=1, max_length=500)
