"""
Capturo — Payment Schemas

Request/response schemas for Razorpay payment integration:
order creation, signature verification, refunds, and webhooks.
All money fields use Decimal (never float) per Rule Set 9.
"""

from typing import Optional
from decimal import Decimal

from pydantic import BaseModel, ConfigDict
from app.schemas.common import DateTimeUTC


# ── Request Schemas ──────────────────────────────────────────────────

class CreatePaymentOrderRequest(BaseModel):
    """Request to create a Razorpay order for a booking."""
    booking_id: str


class VerifyPaymentRequest(BaseModel):
    """Razorpay payment signature verification payload from client."""
    order_id: str
    payment_id: str
    signature: str


class RefundRequest(BaseModel):
    """Manual refund initiation (admin or cancellation trigger)."""
    amount: Decimal
    reason: str


# ── Response Schemas ─────────────────────────────────────────────────

class PaymentOrderResponse(BaseModel):
    """Razorpay order details returned to client for checkout."""
    order_id: str
    amount: int  # in paise (INR × 100)
    currency: str
    key_id: str


class PaymentResponse(BaseModel):
    """Full payment record detail."""
    id: str
    booking_id: str
    payer_id: str
    amount: Decimal
    refund_amount: Decimal
    currency: str
    gateway: str
    gateway_order_id: Optional[str] = None
    gateway_payment_id: Optional[str] = None
    status: str
    captured_at: Optional[DateTimeUTC] = None
    refunded_at: Optional[DateTimeUTC] = None
    created_at: DateTimeUTC
    model_config = ConfigDict(from_attributes=True)


class VerifyPaymentResponse(BaseModel):
    """Result of payment signature verification."""
    verified: bool
    booking_status: str


class RefundResponse(BaseModel):
    """Refund initiation result."""
    refund_id: str
    status: str
    amount: Decimal
    refund_amount: Decimal
    refund_eta_days: int = 5


class WebhookPayload(BaseModel):
    """Razorpay webhook event payload (parsed from request body)."""
    event: str
    payload: dict
