"""
Capturo — Message (Chat) Routes

POST   /messages                     — Send a message
GET    /messages/unread-count        — Get unread count
GET    /messages/{booking_id}        — Get chat history
PATCH  /messages/{message_id}/read   — Mark single message as read
DELETE /messages/{message_id}        — Delete own message (within 1 min)

IMPORTANT: /unread-count is defined BEFORE /{booking_id} to avoid
FastAPI matching "unread-count" as a booking_id parameter.
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.message import MessageResponse, SendMessageRequest, UnreadCountResponse
from app.crud.crud_message import crud_message
from app.services.message_service import message_service

router = APIRouter()


@router.post("", response_model=SuccessResponse[MessageResponse], status_code=201)
def send_message(
    data: SendMessageRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Send a message in a booking chat thread."""
    msg = message_service.send_message(db, sender_id=current_user.id, data=data)
    return SuccessResponse(
        message="Message sent successfully",
        data=MessageResponse.model_validate(msg),
    )


# MUST be defined BEFORE /{booking_id} to avoid path collision
@router.get("/unread-count", response_model=SuccessResponse[UnreadCountResponse])
def get_unread(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get count of unread messages for current user."""
    count = crud_message.get_unread_count(db, user_id=current_user.id)
    return SuccessResponse(
        message="Unread messages count",
        data=UnreadCountResponse(unread_count=count),
    )


@router.get("/{booking_id}", response_model=PaginatedResponse[MessageResponse])
def get_chat(
    booking_id: str,
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get full chat history for a booking."""
    skip = (page - 1) * per_page
    items, total = crud_message.get_chat_history(
        db, booking_id=booking_id, skip=skip, limit=per_page
    )
    messages_data = [MessageResponse.model_validate(i) for i in items]

    # Mark messages as read for the current user
    crud_message.mark_all_as_read(db, booking_id=booking_id, receiver_id=current_user.id)

    return PaginatedResponse(
        items=messages_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.patch("/{message_id}/read", response_model=SuccessResponse[MessageResponse])
def mark_message_read(
    message_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Mark a single message as read (updates read_at timestamp)."""
    msg = message_service.mark_message_read(db, message_id=message_id, user_id=current_user.id)
    return SuccessResponse(
        message="Message marked as read",
        data=MessageResponse.model_validate(msg),
    )


@router.delete("/{message_id}", response_model=SuccessResponse[dict])
def delete_message(
    message_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Delete own message (only within 1 minute of sending)."""
    message_service.delete_message(db, message_id=message_id, user_id=current_user.id)
    return SuccessResponse(message="Message deleted", data={})
