"""
Capturo — Payment Routes

POST /payments/create-order        — Create Razorpay order
POST /payments/verify              — Verify Razorpay signature
GET  /payments/{payment_id}        — Get payment details
POST /payments/{payment_id}/refund — Initiate refund
POST /payments/webhook             — Razorpay webhook receiver
GET  /payments/history             — Payment history for current user
"""

from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user, require_role
from app.core.exceptions import ResourceNotFoundError, PaymentVerificationError
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.payment import (
    CreatePaymentOrderRequest,
    VerifyPaymentRequest,
    VerifyPaymentResponse,
    PaymentResponse,
    PaymentOrderResponse,
    RefundRequest,
)
from app.services.payment_service import payment_service
from app.crud.crud_payment import crud_payment
from app.crud.crud_booking import crud_booking
from app.core.config import settings

router = APIRouter()


@router.post("/create-order", response_model=SuccessResponse[PaymentOrderResponse])
def create_order(
    data: CreatePaymentOrderRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Create a Razorpay order for a booking."""
    booking = crud_booking.get(db, data.booking_id)
    if not booking:
        raise ResourceNotFoundError("Booking", data.booking_id)

    order = payment_service.create_order(booking.total_amount, booking.id)
    return SuccessResponse(
        message="Payment order created",
        data=PaymentOrderResponse(
            order_id=order["id"],
            amount=order["amount"],
            currency=order["currency"],
            key_id=settings.RAZORPAY_KEY_ID,
        ),
    )


@router.post("/verify", response_model=SuccessResponse[VerifyPaymentResponse])
def verify_payment(
    data: VerifyPaymentRequest,
    current_user: User = Depends(get_current_active_user),
):
    """Verify Razorpay payment signature after client-side checkout."""
    verified = payment_service.verify_payment_signature(
        order_id=data.order_id,
        payment_id=data.payment_id,
        signature=data.signature,
    )
    return SuccessResponse(
        message="Payment verified successfully" if verified else "Payment verification failed",
        data=VerifyPaymentResponse(
            verified=verified,
            booking_status="confirmed" if verified else "failed",
        ),
    )


@router.get("/history", response_model=PaginatedResponse[PaymentResponse])
def get_payment_history(
    page: int = Query(1, ge=1),
    per_page: int = Query(10, ge=1, le=100),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get payment history for the current user."""
    skip = (page - 1) * per_page
    items, total = payment_service.get_payment_history(
        db, user_id=current_user.id, skip=skip, limit=per_page
    )
    payments_data = [PaymentResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=payments_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.get("/{payment_id}", response_model=SuccessResponse[PaymentResponse])
def get_payment(
    payment_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get payment details by ID."""
    payment = payment_service.get_payment_details(db, payment_id)
    if not payment:
        raise ResourceNotFoundError("Payment", payment_id)
    return SuccessResponse(
        message="Payment details retrieved",
        data=PaymentResponse.model_validate(payment),
    )


@router.post("/{payment_id}/refund", response_model=SuccessResponse[dict])
def refund_payment(
    payment_id: str,
    data: RefundRequest,
    current_user: User = Depends(require_role("admin")),
    db: Session = Depends(get_db),
):
    """Initiate a refund (admin or cancellation trigger)."""
    payment = crud_payment.get(db, payment_id)
    if not payment:
        raise ResourceNotFoundError("Payment", payment_id)

    refund = payment_service.initiate_refund(payment.gateway_payment_id, data.amount)
    return SuccessResponse(
        message="Refund initiated successfully",
        data={
            "refund_id": refund.get("id", ""),
            "status": "pending",
            "amount": str(data.amount),
        },
    )


@router.post("/webhook", response_model=dict)
async def razorpay_webhook(request: Request, db: Session = Depends(get_db)):
    """Razorpay webhook receiver (HMAC verified, no auth required)."""
    body = await request.body()
    signature = request.headers.get("X-Razorpay-Signature", "")

    if not payment_service.verify_webhook_signature(body, signature):
        raise PaymentVerificationError("Invalid webhook signature")

    import json
    payload = json.loads(body)
    event = payload.get("event", "")

    payment_service.process_webhook(db, event, payload.get("payload", {}))
    return {"status": "ok"}
