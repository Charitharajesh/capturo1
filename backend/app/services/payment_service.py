"""
Capturo — Payment Service

Business logic for Razorpay payment integration:
- Order creation (INR → paise conversion)
- Signature verification (HMAC-SHA256)
- Refund calculation and initiation
- Webhook processing

All money operations use Decimal (Rule Set 9).
"""

import hmac
import hashlib
from decimal import Decimal
from datetime import datetime, timezone
from typing import List, Optional

import razorpay
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import PaymentFailedError, PaymentVerificationError
from app.crud.crud_payment import crud_payment
from app.models.booking import Booking
from app.models.payment import Payment
from app.utils.logger import logger


class PaymentService:
    """Razorpay payment gateway integration."""

    def __init__(self) -> None:
        self.client = razorpay.Client(
            auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET)
        )

    def create_order(self, amount_inr: Decimal, booking_id: str) -> dict:
        """Create a Razorpay order. Amount is converted from INR to paise (×100).

        Args:
            amount_inr: Amount in INR (Decimal).
            booking_id: Associated booking ID for receipt.

        Returns:
            Razorpay order dict containing 'id', 'amount', 'currency', etc.

        Raises:
            PaymentFailedError: If Razorpay API call fails.
        """
        try:
            receipt_id = str(booking_id).strip()
            if len(receipt_id) > 40:
                receipt_id = receipt_id[-40:]
            order = self.client.order.create({
                "amount": int(amount_inr * 100),  # paise
                "currency": "INR",
                "receipt": receipt_id,
                "notes": {"booking_id": str(booking_id)},
            })
            logger.info("razorpay_order_created", order_id=order["id"], amount_paise=int(amount_inr * 100))
            return order
        except Exception as e:
            logger.error("razorpay_order_failed", error=str(e), booking_id=str(booking_id))
            raise PaymentFailedError(f"Razorpay order creation failed: {str(e)}")

    def verify_payment_signature(
        self, order_id: str, payment_id: str, signature: str
    ) -> bool:
        """Verify Razorpay payment signature using HMAC-SHA256.

        ALWAYS verify before marking payment as captured (Rule Set 9).
        Uses constant-time comparison to prevent timing attacks.
        """
        try:
            expected = hmac.new(
                settings.RAZORPAY_KEY_SECRET.encode(),
                f"{order_id}|{payment_id}".encode(),
                hashlib.sha256,
            ).hexdigest()
            return hmac.compare_digest(expected, signature)
        except Exception:
            raise PaymentVerificationError("Could not verify payment signature")

    def calculate_refund(self, booking: Booking) -> Decimal:
        """Calculate refund amount based on cancellation policy.

        Policy (Rule Set 9):
        - >48 hours before event: 100% refund
        - 12–48 hours before event: 50% refund
        - <12 hours before event: no refund
        """
        event_dt = datetime.combine(
            booking.event_date, booking.start_time, tzinfo=timezone.utc
        )
        now = datetime.now(timezone.utc)
        hours_remaining = (event_dt - now).total_seconds() / 3600

        if hours_remaining > 48:
            return booking.total_amount
        elif hours_remaining > 12:
            return booking.total_amount * Decimal("0.50")
        else:
            return Decimal("0.00")

    def initiate_refund(self, gateway_payment_id: str, amount_inr: Decimal) -> dict:
        """Initiate a refund via Razorpay API.

        Args:
            gateway_payment_id: Razorpay payment ID.
            amount_inr: Refund amount in INR (converted to paise).

        Returns:
            Razorpay refund dict.

        Raises:
            PaymentFailedError: If refund API call fails.
        """
        try:
            refund = self.client.payment.refund(gateway_payment_id, {
                "amount": int(amount_inr * 100),
            })
            logger.info("razorpay_refund_initiated", payment_id=gateway_payment_id, amount=str(amount_inr))
            return refund
        except Exception as e:
            logger.error("razorpay_refund_failed", error=str(e), payment_id=gateway_payment_id)
            raise PaymentFailedError(f"Razorpay refund initiation failed: {str(e)}")

    def get_payment_details(self, db: Session, payment_id: str) -> Optional[Payment]:
        """Get payment record by ID."""
        return crud_payment.get(db, payment_id)

    def get_payment_history(
        self, db: Session, user_id: str, skip: int = 0, limit: int = 20
    ) -> tuple[List[Payment], int]:
        """Get paginated payment history for a user."""
        return crud_payment.get_history_by_user(db, user_id, skip=skip, limit=limit)

    def verify_webhook_signature(self, body: bytes, signature: str) -> bool:
        """Verify Razorpay webhook signature.

        Uses the webhook secret (different from API key secret).
        """
        expected = hmac.new(
            settings.RAZORPAY_WEBHOOK_SECRET.encode(),
            body,
            hashlib.sha256,
        ).hexdigest()
        return hmac.compare_digest(expected, signature)

    def process_webhook(self, db: Session, event: str, payload: dict) -> None:
        """Process a verified Razorpay webhook event.

        Handles:
        - payment.captured → update payment status
        - payment.failed → mark payment as failed
        - refund.created → update refund status
        """
        logger.info("webhook_received", event=event)

        if event == "payment.captured":
            payment_entity = payload.get("payment", {}).get("entity", {})
            gateway_payment_id = payment_entity.get("id")
            if gateway_payment_id:
                payment = crud_payment.get_by_gateway_payment_id(db, gateway_payment_id)
                if payment and payment.status != "captured":
                    payment.status = "captured"
                    payment.captured_at = datetime.now(timezone.utc)
                    db.add(payment)
                    db.commit()
                    logger.info("webhook_payment_captured", payment_id=gateway_payment_id)

        elif event == "payment.failed":
            payment_entity = payload.get("payment", {}).get("entity", {})
            gateway_order_id = payment_entity.get("order_id")
            if gateway_order_id:
                payment = crud_payment.get_by_gateway_order_id(db, gateway_order_id)
                if payment:
                    payment.status = "failed"
                    db.add(payment)
                    db.commit()
                    logger.warning("webhook_payment_failed", order_id=gateway_order_id)


payment_service = PaymentService()
