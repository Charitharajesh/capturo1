"""
Capturo — Review Routes

POST   /reviews                        — Submit review for completed booking
GET    /reviews/creator/{creator_id}   — List reviews for a creator
GET    /reviews/{review_id}            — Get single review detail
PATCH  /reviews/{review_id}            — Edit own review (within 24h)
DELETE /reviews/{review_id}            — Delete own review
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.review import CreateReviewRequest, UpdateReviewRequest, ReviewResponse
from app.services.review_service import review_service
from app.crud.crud_review import crud_review

router = APIRouter()


@router.post("", response_model=SuccessResponse[ReviewResponse], status_code=201)
def submit_review(
    data: CreateReviewRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Submit a review for a completed booking (attendee only)."""
    review = review_service.submit_review(
        db,
        reviewer_id=current_user.id,
        booking_id=data.booking_id,
        rating=data.rating,
        comment=data.comment,
    )
    return SuccessResponse(
        message="Review submitted successfully",
        data=ReviewResponse.model_validate(review),
    )


@router.get("/creator/{creator_id}", response_model=PaginatedResponse[ReviewResponse])
def get_creator_reviews(
    creator_id: str,
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """List all verified reviews for a creator (public, no auth)."""
    skip = (page - 1) * per_page
    items, total = crud_review.get_by_creator(
        db, creator_id=creator_id, skip=skip, limit=per_page
    )
    reviews_data = [ReviewResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=reviews_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.get("/{review_id}", response_model=SuccessResponse[ReviewResponse])
def get_review_detail(
    review_id: str,
    db: Session = Depends(get_db),
):
    """Get a single review detail (public)."""
    review = review_service.get_review_detail(db, review_id)
    return SuccessResponse(
        message="Review retrieved",
        data=ReviewResponse.model_validate(review),
    )


@router.patch("/{review_id}", response_model=SuccessResponse[ReviewResponse])
def edit_review(
    review_id: str,
    data: UpdateReviewRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Edit own review (allowed within 24 hours of submission)."""
    review = review_service.update_review(
        db, review_id=review_id, user_id=current_user.id, data=data
    )
    return SuccessResponse(
        message="Review updated successfully",
        data=ReviewResponse.model_validate(review),
    )


@router.delete("/{review_id}", response_model=SuccessResponse[dict])
def delete_review(
    review_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Delete own review or admin moderation."""
    review_service.delete_review(
        db,
        review_id=review_id,
        user_id=current_user.id,
        is_admin=(current_user.role == "admin"),
    )
    return SuccessResponse(message="Review removed", data={})
