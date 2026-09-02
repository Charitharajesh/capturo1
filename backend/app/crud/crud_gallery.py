"""
Capturo — Gallery CRUD Operations

Database operations for the GalleryItem model including portfolio
listing, client delivery, and item management.
"""

from typing import List, Optional

from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.gallery import GalleryItem
from app.schemas.gallery import UpdateGalleryItemRequest


class CreateGalleryItemInternal(BaseModel):
    """Internal schema for gallery item creation (bypasses file upload form)."""

    creator_id: str
    booking_id: Optional[str] = None
    file_url: str
    thumbnail_url: Optional[str] = None
    file_type: str
    file_size_bytes: int
    title: str
    description: str
    is_portfolio: bool = False
    is_client_delivery: bool = False


class CRUDGallery(CRUDBase[GalleryItem, CreateGalleryItemInternal, UpdateGalleryItemRequest]):
    """CRUD operations for the GalleryItem model."""

    def get_portfolio_by_creator(
        self, db: Session, creator_id: str, file_type: Optional[str] = None, skip: int = 0, limit: int = 20
    ) -> tuple[List[GalleryItem], int]:
        """Get paginated portfolio items for a creator."""
        query = db.query(self.model).filter(
            self.model.creator_id == creator_id,
            self.model.is_portfolio == True,
        )
        if file_type:
            query = query.filter(self.model.file_type == file_type)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def get_delivery_by_booking(self, db: Session, booking_id: str) -> List[GalleryItem]:
        """Get all client delivery items for a booking."""
        return db.query(self.model).filter(
            self.model.booking_id == booking_id,
            self.model.is_client_delivery == True,
        ).order_by(self.model.created_at.asc()).all()

    def delete_item(self, db: Session, item_id: str) -> bool:
        """Permanently delete a gallery item record."""
        item = self.get(db, item_id)
        if item:
            db.delete(item)
            db.commit()
            return True
        return False


crud_gallery = CRUDGallery(GalleryItem)
