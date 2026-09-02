"""
Capturo — Admin Routes

GET    /admin/dashboard/stats                      — Platform overview
GET    /admin/bookings                             — All bookings with filters
PATCH  /admin/bookings/{booking_id}/resolve         — Resolve disputed booking
GET    /admin/creators/pending                      — Pending verifications
POST   /admin/creators/{creator_id}/verify          — Verify creator
GET    /admin/users                                — List all users
PATCH  /admin/users/{user_id}/status               — Activate/deactivate user
DELETE /admin/users/{user_id}                       — Hard delete user
POST   /admin/notifications/broadcast              — Broadcast notification
"""

from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import require_role
from app.core.exceptions import ResourceNotFoundError
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.user import UserResponse, UserStatusUpdateRequest
from app.schemas.booking import BookingBriefResponse
from app.schemas.notification import BroadcastNotificationRequest
from app.crud.crud_user import crud_user
from app.services.admin_service import admin_service

router = APIRouter()


@router.get("/dashboard/stats", response_model=SuccessResponse[dict])
def get_stats(
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Platform overview: users, bookings, revenue, growth."""
    stats = admin_service.get_dashboard_stats(db)
    return SuccessResponse(message="Admin statistics loaded successfully", data=stats)


@router.get("/bookings", response_model=PaginatedResponse[BookingBriefResponse])
def get_all_bookings(
    status: Optional[str] = Query(None),
    date_from: Optional[str] = Query(None),
    date_to: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """All bookings with full filter options."""
    skip = (page - 1) * per_page
    items, total = admin_service.get_all_bookings(
        db, status=status, date_from=date_from, date_to=date_to, skip=skip, limit=per_page
    )
    bookings_data = [BookingBriefResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=bookings_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.patch("/bookings/{booking_id}/resolve", response_model=SuccessResponse[BookingBriefResponse])
def resolve_dispute(
    booking_id: str,
    resolution: str = Query(...),
    notes: str = Query(""),
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Resolve a disputed booking."""
    booking = admin_service.resolve_dispute(db, booking_id, resolution, notes)
    return SuccessResponse(
        message="Dispute resolved successfully",
        data=BookingBriefResponse.model_validate(booking),
    )


@router.get("/creators/pending", response_model=PaginatedResponse[UserResponse])
def get_pending_creators(
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Creators pending verification."""
    skip = (page - 1) * per_page
    items, total = admin_service.get_pending_creators(db, skip=skip, limit=per_page)
    users_data = [UserResponse.model_validate(u) for u in items]

    return PaginatedResponse(
        items=users_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.post("/creators/{creator_id}/verify", response_model=SuccessResponse[dict])
def verify_creator(
    creator_id: str,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Verify/approve a creator profile."""
    admin_service.verify_creator(db, creator_id)
    return SuccessResponse(
        message="Creator verified successfully",
        data={"creator_id": creator_id, "is_verified": True},
    )


@router.get("/users", response_model=PaginatedResponse[UserResponse])
def list_users(
    role: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """List all users (admin only, paginated)."""
    skip = (page - 1) * per_page
    items, total = crud_user.get_multi_with_filters(
        db, role=role, skip=skip, limit=per_page
    )
    users_data = [UserResponse.model_validate(u) for u in items]

    return PaginatedResponse(
        items=users_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.patch("/users/{user_id}/status", response_model=SuccessResponse[UserResponse])
def update_user_status(
    user_id: str,
    data: UserStatusUpdateRequest,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Activate/deactivate user (admin)."""
    user = crud_user.get(db, user_id)
    if not user:
        raise ResourceNotFoundError("User", user_id)

    updated = crud_user.update(db, db_obj=user, obj_in={"is_active": data.is_active})
    return SuccessResponse(
        message="User status updated",
        data=UserResponse.model_validate(updated),
    )


@router.delete("/users/{user_id}", response_model=SuccessResponse[dict])
def hard_delete_user(
    user_id: str,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Hard delete a user and cascade records."""
    admin_service.hard_delete_user(db, user_id)
    return SuccessResponse(message="User permanently deleted", data={})


@router.post("/notifications/broadcast", response_model=SuccessResponse[dict])
def broadcast_notification(
    data: BroadcastNotificationRequest,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Send notification to all users or a segment."""
    count = admin_service.broadcast_notification(
        db, title=data.title, body=data.body, target=data.target
    )
    return SuccessResponse(
        message="Broadcast sent successfully",
        data={"sent_count": count},
    )
