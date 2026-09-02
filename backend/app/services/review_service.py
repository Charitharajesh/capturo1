"""
Capturo — Review Service

Business logic for the review and rating system:
- Submit review (validates booking completed, one review per booking)
- Edit review (within 24 hours of creation)
- Delete review (ownership check)
- Recalculate creator average rating after mutations
"""

from datetime import datetime, timezone, timedelta

from sqlalchemy.orm import Session

from app.core.exceptions import (
    ResourceNotFoundError,
    ForbiddenError,
    ReviewNotAllowedError,
    BookingConflictError,
)
from app.crud.crud_review import crud_review
from app.crud.crud_booking import crud_booking
from app.crud.crud_creator import crud_creator
from app.models.review import Review
from app.schemas.review import UpdateReviewRequest
from app.utils.logger import logger


class ReviewService:
    """Review and rating management business logic."""

    def submit_review(
        self, db: Session, reviewer_id: str, booking_id: str, rating: int, comment: str = None
    ) -> Review:
        """Submit a review for a completed booking.

        Validates:
        1. Booking exists and is completed
        2. Reviewer is the booking attendee
        3. No existing review for this booking

        After creation, recalculates the creator's average rating.
        """
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.attendee_id != reviewer_id:
            raise ForbiddenError("Only the booking attendee can write reviews.")

        if booking.status not in ["completed", "paid", "confirmed"]:
            raise ReviewNotAllowedError("Reviews are only allowed after completed or confirmed bookings.")

        # Check for duplicate review
        existing = crud_review.get_by_booking_id(db, booking_id)
        if existing:
            raise BookingConflictError("A review has already been submitted for this booking.")

        # Create review
        db_review = Review(
            booking_id=booking_id,
            reviewer_id=reviewer_id,
            creator_id=booking.creator_id,
            rating=rating,
            comment=comment,
            is_verified=True,
        )
        db.add(db_review)
        db.commit()
        db.refresh(db_review)

        # Recalculate creator average
        self._recalculate_creator_rating(db, booking.creator_id)

        logger.info("review_submitted", review_id=db_review.id, rating=rating)
        return db_review

    def update_review(
        self, db: Session, review_id: str, user_id: str, data: UpdateReviewRequest
    ) -> Review:
        """Edit a review (allowed within 24 hours of creation only).

        Validates:
        1. Review exists
        2. User owns the review
        3. Within 24-hour edit window
        """
        review = crud_review.get(db, review_id)
        if not review:
            raise ResourceNotFoundError("Review", review_id)

        if review.reviewer_id != user_id:
            raise ForbiddenError("You can only edit your own reviews.")

        # Check 24-hour edit window
        time_since = datetime.now(timezone.utc) - review.created_at.replace(tzinfo=timezone.utc)
        if time_since > timedelta(hours=24):
            raise ForbiddenError("Reviews can only be edited within 24 hours of submission.")

        update_data = data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(review, field, value)

        db.add(review)
        db.commit()
        db.refresh(review)

        # Recalculate if rating changed
        if "rating" in update_data:
            self._recalculate_creator_rating(db, review.creator_id)

        logger.info("review_updated", review_id=review.id)
        return review

    def delete_review(self, db: Session, review_id: str, user_id: str, is_admin: bool = False) -> None:
        """Delete a review (owner or admin only).

        After deletion, recalculates the creator's average rating.
        """
        review = crud_review.get(db, review_id)
        if not review:
            raise ResourceNotFoundError("Review", review_id)

        if not is_admin and review.reviewer_id != user_id:
            raise ForbiddenError("You can only delete your own reviews.")

        creator_id = review.creator_id
        db.delete(review)
        db.commit()

        # Recalculate after deletion
        self._recalculate_creator_rating(db, creator_id)
        logger.info("review_deleted", review_id=review_id)

    def get_review_detail(self, db: Session, review_id: str) -> Review:
        """Get a single review by ID."""
        review = crud_review.get(db, review_id)
        if not review:
            raise ResourceNotFoundError("Review", review_id)
        return review

    def _recalculate_creator_rating(self, db: Session, creator_id: str) -> None:
        """Recalculate and persist a creator's average rating and review count."""
        avg_rating, count = crud_review.get_creator_average_rating(db, creator_id)
        creator_profile = crud_creator.get_by_user_id(db, creator_id)
        if creator_profile:
            creator_profile.avg_rating = avg_rating
            creator_profile.total_reviews = count
            db.add(creator_profile)
            db.commit()


review_service = ReviewService()
