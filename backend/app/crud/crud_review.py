"""
Capturo — Review CRUD Operations

Database operations for the Review model.
"""

from typing import List, Optional

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.review import Review
from app.schemas.review import CreateReviewRequest, UpdateReviewRequest


class CRUDReview(CRUDBase[Review, CreateReviewRequest, UpdateReviewRequest]):
    """CRUD operations for the Review model."""

    def get_by_booking_id(self, db: Session, booking_id: str) -> Optional[Review]:
        """Get the review for a specific booking (one-to-one)."""
        return db.query(self.model).filter(self.model.booking_id == booking_id).first()

    def get_by_creator(
        self, db: Session, creator_id: str, skip: int = 0, limit: int = 20
    ) -> tuple[List[Review], int]:
        """Get paginated reviews for a creator, ordered by created_at DESC."""
        query = db.query(self.model).filter(self.model.creator_id == creator_id)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def get_by_reviewer(
        self, db: Session, reviewer_id: str, skip: int = 0, limit: int = 20
    ) -> tuple[List[Review], int]:
        """Get paginated reviews written by a specific user."""
        query = db.query(self.model).filter(self.model.reviewer_id == reviewer_id)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def get_creator_average_rating(self, db: Session, creator_id: str) -> tuple[float, int]:
        """Calculate average rating and total review count for a creator."""
        result = db.query(
            func.avg(self.model.rating),
            func.count(self.model.id),
        ).filter(self.model.creator_id == creator_id).first()
        avg_rating = float(result[0]) if result[0] is not None else 0.0
        count = int(result[1]) if result[1] is not None else 0
        return avg_rating, count


crud_review = CRUDReview(Review)
