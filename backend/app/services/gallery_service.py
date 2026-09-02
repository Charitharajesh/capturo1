"""
Capturo — Gallery Service

Business logic for media gallery management:
- Upload with file validation, save, and thumbnail generation
- Update metadata
- Delete item with file cleanup
- Shareable delivery links
- Zip download for client delivery
"""

import os
from datetime import datetime, timezone, timedelta
from decimal import Decimal
from typing import Optional

from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import ResourceNotFoundError, ForbiddenError
from app.crud.crud_gallery import crud_gallery, CreateGalleryItemInternal
from app.models.gallery import GalleryItem
from app.schemas.gallery import UpdateGalleryItemRequest
from app.services.upload_service import upload_service
from app.utils.logger import logger


class GalleryService:
    """Media gallery management business logic."""

    async def upload_gallery_item(
        self,
        db: Session,
        file: UploadFile,
        creator_id: str,
        booking_id: Optional[str] = None,
        title: str = "",
        description: str = "",
        is_portfolio: bool = False,
        is_client_delivery: bool = False,
    ) -> GalleryItem:
        """Upload a file and create a gallery item record.

        1. Validate and save file via upload_service
        2. Determine file type and size
        3. Create GalleryItem record with accurate metadata
        """
        file_url, thumbnail_url = await upload_service.save_uploaded_file(
            file, creator_id=creator_id, booking_id=booking_id
        )

        # Get actual file size
        file.file.seek(0, 2)  # seek to end
        file_size = file.file.tell()
        file.file.seek(0)

        file_type = "video" if file.content_type and file.content_type.startswith("video/") else "photo"

        item_in = CreateGalleryItemInternal(
            creator_id=creator_id,
            booking_id=booking_id,
            file_url=file_url,
            thumbnail_url=thumbnail_url,
            file_type=file_type,
            file_size_bytes=file_size,
            title=title,
            description=description,
            is_portfolio=is_portfolio,
            is_client_delivery=is_client_delivery,
        )

        db_item = crud_gallery.create(db, obj_in=item_in)
        logger.info("gallery_item_uploaded", item_id=db_item.id, type=file_type, size=file_size)
        return db_item

    def update_gallery_item(
        self, db: Session, item_id: str, user_id: str, data: UpdateGalleryItemRequest
    ) -> GalleryItem:
        """Update gallery item metadata.

        Validates:
        1. Item exists
        2. User is the item's creator
        """
        item = crud_gallery.get(db, item_id)
        if not item:
            raise ResourceNotFoundError("GalleryItem", item_id)

        if item.creator_id != user_id:
            raise ForbiddenError("You can only edit your own gallery items.")

        updated = crud_gallery.update(db, db_obj=item, obj_in=data)
        return updated

    def delete_gallery_item(self, db: Session, item_id: str, user_id: str) -> None:
        """Delete a gallery item and remove files from disk.

        Validates:
        1. Item exists
        2. User is the item's creator
        """
        item = crud_gallery.get(db, item_id)
        if not item:
            raise ResourceNotFoundError("GalleryItem", item_id)

        if item.creator_id != user_id:
            raise ForbiddenError("You can only delete your own gallery items.")

        # Clean up files from disk
        self._cleanup_file(item.file_url)
        if item.thumbnail_url and item.thumbnail_url != item.file_url:
            self._cleanup_file(item.thumbnail_url)

        crud_gallery.delete_item(db, item_id)
        logger.info("gallery_item_deleted", item_id=item_id)

    def create_share_link(
        self, db: Session, booking_id: str, user_id: str, expires_in_days: int = 30
    ) -> dict:
        """Generate a shareable public link for client delivery.

        Returns:
            Dict with share_url and expires_at.
        """
        items = crud_gallery.get_delivery_by_booking(db, booking_id)
        if not items:
            raise ResourceNotFoundError("GalleryDelivery", booking_id)

        # Validate user is part of the booking
        first_item = items[0]
        if first_item.creator_id != user_id:
            raise ForbiddenError("Only the creator can generate share links.")

        expires_at = datetime.now(timezone.utc) + timedelta(days=expires_in_days)
        # In production, generate a signed URL or short code
        share_url = f"{settings.APP_HOST}/shared/delivery/{booking_id}"

        return {
            "share_url": share_url,
            "expires_at": expires_at,
        }

    def _cleanup_file(self, file_url: str) -> None:
        """Remove a file from disk based on its URL path."""
        try:
            # file_url format: /uploads/creator_id/booking_id/filename.ext
            relative_path = file_url.lstrip("/")
            full_path = os.path.join(settings.UPLOAD_DIR, relative_path.replace("uploads/", "", 1))
            if os.path.exists(full_path):
                os.remove(full_path)
        except Exception as e:
            logger.warning("file_cleanup_failed", file_url=file_url, error=str(e))


gallery_service = GalleryService()
