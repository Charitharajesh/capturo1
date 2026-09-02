"""
Capturo — Notification Routes

GET    /notifications                         — List user notifications
GET    /notifications/unread-count            — Badge count
PATCH  /notifications/{notification_id}/read  — Mark single as read
POST   /notifications/read-all               — Mark all as read
DELETE /notifications/{notification_id}       — Delete notification
"""

from typing import Optional
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.core.exceptions import ResourceNotFoundError
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.notification import NotificationResponse, UpdatedCountResponse
from app.crud.crud_notification import crud_notification

router = APIRouter()


@router.get("", response_model=PaginatedResponse[NotificationResponse])
def get_my_notifications(
    is_read: Optional[bool] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """List all notifications for the current user."""
    skip = (page - 1) * per_page
    items, total = crud_notification.get_by_user(
        db, user_id=current_user.id, is_read=is_read, skip=skip, limit=per_page
    )
    notify_data = [NotificationResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=notify_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


# MUST be before /{notification_id} to avoid path collision
@router.get("/unread-count", response_model=SuccessResponse[dict])
def get_unread(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get badge count for notification bell."""
    count = crud_notification.get_unread_count(db, user_id=current_user.id)
    return SuccessResponse(
        message="Unread notifications count",
        data={"unread_count": count},
    )


@router.patch("/{notification_id}/read", response_model=SuccessResponse[NotificationResponse])
def read_notification(
    notification_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Mark a single notification as read."""
    notification = crud_notification.get(db, notification_id)
    if not notification or notification.user_id != current_user.id:
        raise ResourceNotFoundError("Notification", notification_id)

    notification = crud_notification.mark_as_read(db, notification_id)

    return SuccessResponse(
        message="Notification read",
        data=NotificationResponse.model_validate(notification),
    )


@router.post("/read-all", response_model=SuccessResponse[UpdatedCountResponse])
def read_all(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Mark all notifications as read."""
    count = crud_notification.mark_all_read(db, user_id=current_user.id)
    return SuccessResponse(
        message="All notifications marked as read",
        data=UpdatedCountResponse(updated_count=count),
    )


@router.delete("/{notification_id}", response_model=SuccessResponse[dict])
def delete_notification(
    notification_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Delete a notification."""
    notification = crud_notification.get(db, notification_id)
    if not notification or notification.user_id != current_user.id:
        raise ResourceNotFoundError("Notification", notification_id)

    crud_notification.delete_notification(db, notification_id)
    return SuccessResponse(message="Notification deleted", data={})
