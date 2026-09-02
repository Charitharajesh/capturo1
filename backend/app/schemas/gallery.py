"""
Capturo — Gallery Schemas

Request/response schemas for media gallery and client delivery.
"""

from typing import List, Optional
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field
from app.schemas.common import DateTimeUTC


class GalleryItemResponse(BaseModel):
    """Gallery item (photo or video) detail."""

    id: str
    creator_id: str
    booking_id: Optional[str] = None
    file_url: str
    thumbnail_url: Optional[str] = None
    file_type: str
    file_size_bytes: int
    title: Optional[str] = None
    description: Optional[str] = None
    is_portfolio: bool
    is_client_delivery: bool
    views_count: int
    created_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class UpdateGalleryItemRequest(BaseModel):
    """Update gallery item metadata."""

    title: Optional[str] = Field(None, max_length=200)
    description: Optional[str] = Field(None, max_length=1000)
    is_portfolio: Optional[bool] = None
    is_client_delivery: Optional[bool] = None


class CreateShareLinkRequest(BaseModel):
    """Generate a shareable link for client delivery."""

    expires_in_days: int = Field(30, ge=1, le=365)


class ShareLinkResponse(BaseModel):
    """Shareable link with expiration."""

    share_url: str
    expires_at: DateTimeUTC


class DownloadResponse(BaseModel):
    """Download URL for zipped delivery files."""

    download_url: str
    size_bytes: int


class ClientDeliveryGalleryResponse(BaseModel):
    """Client delivery gallery with aggregate stats."""

    items: List[GalleryItemResponse]
    total: int
    total_size_gb: Decimal
