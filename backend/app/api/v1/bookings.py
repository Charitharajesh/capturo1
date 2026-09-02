"""
Capturo — Booking Routes

POST   /bookings                       — Create booking with payment intent
GET    /bookings                       — List bookings for current user
GET    /bookings/statement             — Download PDF payment statement
GET    /bookings/statement/summary     — Get AI payment summary
GET    /bookings/{booking_id}          — Get full booking details
PATCH  /bookings/{booking_id}          — Update booking before confirmation
POST   /bookings/{booking_id}/confirm  — Confirm after payment
POST   /bookings/{booking_id}/complete — Mark completed (creator only)
POST   /bookings/{booking_id}/cancel   — Cancel with refund logic
POST   /bookings/{booking_id}/dispute  — Raise a dispute
GET    /bookings/{booking_id}/invoice  — Download booking invoice PDF
DELETE /bookings/{booking_id}          — Admin hard delete
"""

import os
from typing import Optional

from fastapi import APIRouter, Depends, Query
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user, require_role
from app.core.exceptions import ResourceNotFoundError, ForbiddenError
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.booking import (
    CreateBookingRequest,
    UpdateBookingRequest,
    BookingResponse,
    BookingBriefResponse,
    BookingCreatedResponse,
    ConfirmBookingPaymentRequest,
    CancelBookingRequest,
    CancelBookingResponse,
    CompleteBookingRequest,
    DisputeBookingRequest,
)
from app.services.booking_service import booking_service
from app.crud.crud_booking import crud_booking
from app.core.config import settings

router = APIRouter()


@router.post("", response_model=SuccessResponse[BookingCreatedResponse], status_code=201)
def create_booking(
    data: CreateBookingRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Create a new booking request with Razorpay payment intent."""
    booking, order = booking_service.create_booking(
        db, attendee_id=current_user.id, data=data
    )
    return SuccessResponse(
        message="Booking payment intent created successfully",
        data=BookingCreatedResponse(
            booking_id=booking.id,
            status=booking.status,
            payment_order_id=order["id"],
            payment_key_id=settings.RAZORPAY_KEY_ID,
            total_amount=booking.total_amount,
        ),
    )


@router.get("", response_model=PaginatedResponse[BookingResponse])
def get_my_bookings(
    status: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """List bookings for current user (attendee or creator role-aware)."""
    skip = (page - 1) * per_page
    if current_user.role == "creator":
        items, total = crud_booking.get_by_creator(
            db, creator_id=current_user.id, status=status, skip=skip, limit=per_page
        )
    else:
        items, total = crud_booking.get_by_attendee(
            db, attendee_id=current_user.id, status=status, skip=skip, limit=per_page
        )

    bookings_data = [BookingResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=bookings_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


# ── STATIC ROUTES (must be BEFORE /{booking_id} parametric routes) ──


@router.get("/statement")
def get_payout_statement(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Generate and download a comprehensive monthly/yearly PDF payment statement."""
    from app.models.booking import Booking
    if current_user.role == "creator":
        bookings = db.query(Booking).filter(
            Booking.creator_id == current_user.id,
            Booking.status.in_(["confirmed", "paid", "completed"])
        ).order_by(Booking.event_date.desc()).all()
    else:
        bookings = db.query(Booking).filter(
            Booking.attendee_id == current_user.id,
            Booking.status.in_(["confirmed", "paid", "completed"])
        ).order_by(Booking.event_date.desc()).all()
    
    statement_dir = os.path.join("media", "statement")
    os.makedirs(statement_dir, exist_ok=True)
    filename = f"statement_{current_user.id}.pdf"
    file_path = os.path.join(statement_dir, filename)
    
    from app.services.invoice_service import invoice_service
    invoice_service.generate_statement_pdf(current_user, bookings, file_path)
    
    return FileResponse(
        path=file_path,
        media_type="application/pdf",
        filename="Capturo_Payment_Statement.pdf"
    )


@router.get("/statement/summary")
def get_payout_statement_summary(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Generate and return AI summary metrics of payments for the authenticated user."""
    from app.models.booking import Booking
    if current_user.role == "creator":
        bookings = db.query(Booking).filter(
            Booking.creator_id == current_user.id,
            Booking.status.in_(["confirmed", "paid", "completed"])
        ).all()
        
        total_earned = sum(float(b.total_amount) for b in bookings)
        category_counts = {}
        for b in bookings:
            category_counts[b.event_type] = category_counts.get(b.event_type, 0) + 1
            
        popular_category = max(category_counts, key=category_counts.get) if category_counts else "None"
        avg_earned = total_earned / len(bookings) if bookings else 0.0
        
        summary_text = (
            f"AI Analysis of your photography earnings:\n\n"
            f"• Total Earned: INR {total_earned:,.2f}\n"
            f"• Most popular category: {popular_category.title()}\n"
            f"• Average fee per session: INR {avg_earned:,.2f}\n\n"
            f"Capturo Smart Tip: You generated most of your revenue from {popular_category.title()} photography. "
            f"Recommended: Offer special package deals for {popular_category.title()} to increase booking rates by 15%."
        )
    else:
        bookings = db.query(Booking).filter(
            Booking.attendee_id == current_user.id,
            Booking.status.in_(["confirmed", "paid", "completed"])
        ).all()
        
        total_spent = sum(float(b.total_amount) for b in bookings)
        category_counts = {}
        for b in bookings:
            category_counts[b.event_type] = category_counts.get(b.event_type, 0) + 1
            
        popular_category = max(category_counts, key=category_counts.get) if category_counts else "None"
        avg_spent = total_spent / len(bookings) if bookings else 0.0
        
        summary_text = (
            f"AI Analysis of your photography expenses:\n\n"
            f"• Total Spent: INR {total_spent:,.2f}\n"
            f"• Most booked category: {popular_category.title()}\n"
            f"• Average fee per session: INR {avg_spent:,.2f}\n\n"
            f"Capturo Smart Tip: You spent most of your budget on {popular_category.title()} photography. "
            f"Recommended: Consider subscription plans to unlock 10% cashbacks."
        )
    return SuccessResponse(message="Summary retrieved", data={"summary": summary_text})


# ── PARAMETRIC ROUTES (/{booking_id}) ────────────────────────────────


@router.get("/{booking_id}", response_model=SuccessResponse[BookingResponse])
def get_booking_details(
    booking_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get full booking details (only accessible by booking participants or admin)."""
    booking = crud_booking.get(db, booking_id)
    if not booking:
        raise ResourceNotFoundError("Booking", booking_id)

    # Access control: only attendee, creator, or admin can view
    if (
        current_user.role != "admin"
        and booking.attendee_id != current_user.id
        and booking.creator_id != current_user.id
    ):
        raise ForbiddenError("You don't have access to this booking.")

    return SuccessResponse(
        message="Booking details retrieved",
        data=BookingResponse.model_validate(booking),
    )


@router.patch("/{booking_id}", response_model=SuccessResponse[BookingResponse])
def update_booking(
    booking_id: str,
    data: UpdateBookingRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Update booking notes/details before confirmation (pending only)."""
    updated = booking_service.update_booking(
        db, booking_id=booking_id, user_id=current_user.id, data=data
    )
    return SuccessResponse(
        message="Booking updated successfully",
        data=BookingResponse.model_validate(updated),
    )


@router.post("/{booking_id}/confirm", response_model=SuccessResponse[BookingResponse])
def confirm_booking(
    booking_id: str,
    data: ConfirmBookingPaymentRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Confirm booking after Razorpay payment captured."""
    confirmed_booking = booking_service.confirm_booking(
        db,
        booking_id=booking_id,
        payment_id=data.payment_id,
        signature=data.payment_signature,
    )
    return SuccessResponse(
        message="Booking slot confirmed successfully",
        data=BookingResponse.model_validate(confirmed_booking),
    )


@router.post("/{booking_id}/complete", response_model=SuccessResponse[BookingResponse])
def complete_booking(
    booking_id: str,
    current_user: User = Depends(require_role("creator")),
    db: Session = Depends(get_db),
):
    """Mark booking as completed (creator only)."""
    completed = booking_service.complete_booking(
        db, booking_id=booking_id, creator_user_id=current_user.id
    )
    return SuccessResponse(
        message="Booking completed successfully",
        data=BookingResponse.model_validate(completed),
    )


@router.post("/{booking_id}/cancel", response_model=SuccessResponse[CancelBookingResponse])
def cancel_booking(
    booking_id: str,
    data: CancelBookingRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Cancel booking with refund calculation."""
    cancelled_booking, refund_amount = booking_service.cancel_booking(
        db,
        booking_id=booking_id,
        user_id=current_user.id,
        reason=data.reason,
    )
    return SuccessResponse(
        message="Booking cancelled successfully",
        data=CancelBookingResponse(
            status=cancelled_booking.status,
            refund_amount=refund_amount,
            refund_eta_days=5,
        ),
    )


@router.post("/{booking_id}/dispute", response_model=SuccessResponse[BookingResponse])
def dispute_booking(
    booking_id: str,
    data: DisputeBookingRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Raise a dispute on a completed booking."""
    disputed = booking_service.dispute_booking(
        db, booking_id=booking_id, user_id=current_user.id, reason=data.reason
    )
    return SuccessResponse(
        message="Dispute raised successfully",
        data=BookingResponse.model_validate(disputed),
    )


@router.get("/{booking_id}/invoice")
def get_booking_invoice(
    booking_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Generate and download a professional PDF invoice for the booking."""
    booking = crud_booking.get(db, booking_id)
    if not booking:
        raise ResourceNotFoundError("Booking", booking_id)

    # Access control: only attendee, creator, or admin can download
    if (
        current_user.role != "admin"
        and booking.attendee_id != current_user.id
        and booking.creator_id != current_user.id
    ):
        raise ForbiddenError("You don't have access to this booking's invoice.")

    # Save to local file system
    invoice_dir = os.path.join("media", "invoice")
    os.makedirs(invoice_dir, exist_ok=True)
    filename = f"invoice_{booking.id}.pdf"
    file_path = os.path.join(invoice_dir, filename)

    from app.services.invoice_service import invoice_service
    invoice_service.generate_invoice_pdf(booking, file_path)

    # Update database
    booking.invoice_url = f"/media/invoice/{filename}"
    db.commit()

    return FileResponse(
        path=file_path,
        media_type="application/pdf",
        filename=f"Invoice_{booking.id[:8].upper()}.pdf"
    )


@router.delete("/{booking_id}", response_model=SuccessResponse[dict])
def admin_delete_booking(
    booking_id: str,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Admin hard delete a booking (audit trail preserved in logs)."""
    success = crud_booking.hard_delete(db, booking_id)
    if not success:
        raise ResourceNotFoundError("Booking", booking_id)
    return SuccessResponse(message="Booking deleted", data={})
