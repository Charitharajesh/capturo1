"""
Capturo — Notification CRUD Operations

Database operations for the Notification model including user-scoped
queries, read receipts, and bulk operations.
"""

from typing import List, Optional
from datetime import datetime, timezone

from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.notification import Notification


class CreateNotificationInternal(BaseModel):
    """Internal schema for creating notification records."""

    user_id: str
    title: str
    body: str
    notification_type: str
    reference_id: Optional[str] = None
    reference_type: Optional[str] = None


class CRUDNotification(CRUDBase[Notification, CreateNotificationInternal, CreateNotificationInternal]):
    """CRUD operations for the Notification model."""

    def get_by_user(
        self, db: Session, user_id: str, is_read: Optional[bool] = None, skip: int = 0, limit: int = 20
    ) -> tuple[List[Notification], int]:
        """Get paginated notifications for a user, ordered by created_at DESC."""
        query = db.query(self.model).filter(self.model.user_id == user_id)
        if is_read is not None:
            query = query.filter(self.model.is_read == is_read)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def mark_all_read(self, db: Session, user_id: str) -> int:
        """Mark all unread notifications as read for a user (bulk)."""
        updated = db.query(self.model).filter(
            self.model.user_id == user_id,
            self.model.is_read == False,
        ).update(
            {"is_read": True, "read_at": datetime.now(timezone.utc)},
            synchronize_session=False,
        )
        db.commit()
        return updated

    def get_unread_count(self, db: Session, user_id: str) -> int:
        """Count unread notifications for a user (badge count)."""
        return db.query(self.model).filter(
            self.model.user_id == user_id,
            self.model.is_read == False,
        ).count()

    def mark_as_read(self, db: Session, notification_id: str) -> Optional[Notification]:
        """Mark a single notification as read with UTC timestamp."""
        notification = self.get(db, notification_id)
        if notification:
            notification.is_read = True
            notification.read_at = datetime.now(timezone.utc)
            db.add(notification)
            db.commit()
            db.refresh(notification)
        return notification

    def delete_notification(self, db: Session, notification_id: str) -> bool:
        """Permanently delete a notification record."""
        notification = self.get(db, notification_id)
        if notification:
            db.delete(notification)
            db.commit()
            return True
        return False


crud_notification = CRUDNotification(Notification)
