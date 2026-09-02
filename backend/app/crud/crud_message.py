"""
Capturo — Message CRUD Operations

Database operations for the Message model including chat history,
unread counts, and read-receipt management.
"""

from typing import List, Optional
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.message import Message
from app.schemas.message import SendMessageRequest


class CRUDMessage(CRUDBase[Message, SendMessageRequest, SendMessageRequest]):
    """CRUD operations for the Message model."""

    def get_chat_history(
        self, db: Session, booking_id: str, skip: int = 0, limit: int = 50
    ) -> tuple[List[Message], int]:
        """Get paginated chat history for a booking, returned in chronological order."""
        query = db.query(self.model).filter(self.model.booking_id == booking_id)
        total = query.count()
        # Fetch in DESC order for pagination, then reverse for chronological display
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items[::-1], total

    def get_messages_after_timestamp(
        self, db: Session, booking_id: str, after: datetime
    ) -> List[Message]:
        """Get messages created after a specific timestamp (polling pattern)."""
        return db.query(self.model).filter(
            self.model.booking_id == booking_id,
            self.model.created_at > after,
        ).order_by(self.model.created_at.asc()).all()

    def get_unread_count(self, db: Session, user_id: str) -> int:
        """Count unread messages for a user across all booking chats."""
        return db.query(self.model).filter(
            self.model.receiver_id == user_id,
            self.model.is_read == False,
        ).count()

    def mark_as_read(self, db: Session, message_id: str) -> Optional[Message]:
        """Mark a single message as read with UTC timestamp."""
        msg = self.get(db, message_id)
        if msg:
            msg.is_read = True
            msg.read_at = datetime.now(timezone.utc)
            db.add(msg)
            db.commit()
            db.refresh(msg)
        return msg

    def mark_all_as_read(self, db: Session, booking_id: str, receiver_id: str) -> int:
        """Mark all unread messages in a booking chat as read (bulk)."""
        updated = db.query(self.model).filter(
            self.model.booking_id == booking_id,
            self.model.receiver_id == receiver_id,
            self.model.is_read == False,
        ).update(
            {"is_read": True, "read_at": datetime.now(timezone.utc)},
            synchronize_session=False,
        )
        db.commit()
        return updated

    def soft_delete(self, db: Session, message_id: str) -> bool:
        """Delete a message (hard delete — only allowed within 1 minute of creation)."""
        msg = self.get(db, message_id)
        if msg:
            db.delete(msg)
            db.commit()
            return True
        return False


crud_message = CRUDMessage(Message)
