"""
Capturo — Message Service

Business logic for booking-scoped chat messaging:
- Send message (validates booking participants)
- Mark single message as read
- Delete message (within 1-minute window)
"""

from datetime import datetime, timezone, timedelta

from sqlalchemy.orm import Session

from app.core.exceptions import ResourceNotFoundError, ForbiddenError
from app.crud.crud_message import crud_message
from app.crud.crud_booking import crud_booking
from app.models.message import Message
from app.schemas.message import SendMessageRequest
from app.services.notification_service import notification_service
from app.utils.logger import logger


class MessageService:
    """Chat messaging business logic."""

    def send_message(self, db: Session, sender_id: str, data: SendMessageRequest) -> Message:
        """Send a message in a booking chat thread.

        Validates:
        1. Booking exists
        2. Sender is part of the booking (attendee or creator)

        Creates message and sends push notification to receiver.
        """
        # Validate booking and participants
        booking = crud_booking.get(db, data.booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", data.booking_id)

        if sender_id not in [booking.attendee_id, booking.creator_id]:
            raise ForbiddenError("Only booking participants can send messages.")

        # Resolve receiver dynamically to prevent empty/incorrect IDs from client
        receiver_id = booking.creator_id if sender_id == booking.attendee_id else booking.attendee_id

        # Create message
        msg_data = data.model_dump()
        msg_data["sender_id"] = sender_id
        msg_data["receiver_id"] = receiver_id

        db_msg = Message(**msg_data)
        db.add(db_msg)
        db.commit()
        db.refresh(db_msg)

        # Send push notification to receiver
        notification_service.send_notification(
            db,
            user_id=receiver_id,
            title="New Message",
            body=data.content[:100] if data.content else "Sent a media message",
            notification_type="new_message",
            reference_id=data.booking_id,
            reference_type="message",
        )

        return db_msg

    def mark_message_read(self, db: Session, message_id: str, user_id: str) -> Message:
        """Mark a single message as read.

        Validates:
        1. Message exists
        2. User is the receiver
        """
        msg = crud_message.get(db, message_id)
        if not msg:
            raise ResourceNotFoundError("Message", message_id)

        if msg.receiver_id != user_id:
            raise ForbiddenError("Only the receiver can mark messages as read.")

        msg = crud_message.mark_as_read(db, message_id)
        return msg

    def delete_message(self, db: Session, message_id: str, user_id: str) -> None:
        """Delete a message (only allowed within 1 minute of creation).

        Validates:
        1. Message exists
        2. User is the sender
        3. Within 1-minute deletion window
        """
        msg = crud_message.get(db, message_id)
        if not msg:
            raise ResourceNotFoundError("Message", message_id)

        if msg.sender_id != user_id:
            raise ForbiddenError("You can only delete your own messages.")

        # Check 1-minute window
        time_since = datetime.now(timezone.utc) - msg.created_at.replace(tzinfo=timezone.utc)
        if time_since > timedelta(minutes=1):
            raise ForbiddenError("Messages can only be deleted within 1 minute of sending.")

        crud_message.soft_delete(db, message_id)
        logger.info("message_deleted", message_id=message_id, sender_id=user_id)


message_service = MessageService()
