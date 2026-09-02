"""
Capturo — Booking Service

Business logic for the complete booking lifecycle:
create → confirm → complete → cancel → dispute.

All money calculations use Decimal (Rule Set 9).
All datetime operations use timezone-aware UTC (Rule Set 4).
All exceptions are typed (Rule Set 12).
"""

from decimal import Decimal
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import (
    ResourceNotFoundError,
    BookingConflictError,
    CreatorUnavailableError,
    ForbiddenError,
    PaymentVerificationError,
)
from app.crud.crud_booking import crud_booking
from app.crud.crud_creator import crud_creator
from app.crud.crud_user import crud_user
from app.crud.crud_payment import crud_payment
from app.models.booking import Booking
from app.models.payment import Payment
from app.schemas.booking import CreateBookingRequest, UpdateBookingRequest
from app.services.payment_service import payment_service
from app.services.notification_service import notification_service
from app.utils.logger import logger


class BookingService:
    """Booking state machine and business logic orchestrator."""

    def create_booking(
        self, db: Session, attendee_id: str, data: CreateBookingRequest
    ) -> tuple[Booking, dict]:
        """Create a new booking with payment intent.

        Flow:
        1. Validate creator profile exists
        2. Check time-slot availability
        3. Calculate total amount (hourly_rate × duration)
        4. Create booking record with status='pending'
        5. Create Razorpay order
        6. Create Payment record
        7. Notify creator via FCM

        Returns:
            Tuple of (booking, razorpay_order_dict).
        """
        # 1. Get creator profile
        creator_profile = crud_creator.get_by_user_id(db, data.creator_id)
        if not creator_profile:
            raise ResourceNotFoundError("CreatorProfile", data.creator_id)

        # 2. Check availability
        is_available = crud_booking.check_availability(
            db,
            creator_id=data.creator_id,
            event_date=data.event_date,
            start_time=data.start_time,
            duration_hours=float(data.duration_hours),
        )
        if not is_available:
            raise CreatorUnavailableError(
                "Creator is not available at the requested date and time slot."
            )

        # 3. Calculate total price using Decimal
        total_amount = Decimal(str(creator_profile.hourly_rate)) * data.duration_hours

        # 4. Create booking
        booking_data = data.model_dump()
        booking_data["attendee_id"] = attendee_id
        booking_data["total_amount"] = total_amount
        booking_data["status"] = "pending"

        db_booking = Booking(**booking_data)
        db.add(db_booking)
        db.commit()
        db.refresh(db_booking)

        # 5. Create Razorpay order
        order = payment_service.create_order(total_amount, db_booking.id)

        # 6. Save Payment record
        payment = Payment(
            booking_id=db_booking.id,
            payer_id=attendee_id,
            amount=total_amount,
            currency="INR",
            gateway="razorpay",
            gateway_order_id=order["id"],
            status="pending",
        )
        db.add(payment)
        db.commit()
        db.refresh(payment)

        # 7. Notify creator
        attendee = crud_user.get(db, attendee_id)
        attendee_name = attendee.full_name if attendee else "An Attendee"
        notification_service.send_notification(
            db,
            user_id=data.creator_id,
            title="New Booking Request",
            body=f"New booking from {attendee_name} for a {data.event_type} event.",
            notification_type="booking_confirmed",
            reference_id=db_booking.id,
            reference_type="booking",
        )

        logger.info(
            "booking_created",
            booking_id=db_booking.id,
            attendee_id=attendee_id,
            creator_id=data.creator_id,
            amount=str(total_amount),
        )
        return db_booking, order

    def confirm_booking(
        self, db: Session, booking_id: str, payment_id: str, signature: str
    ) -> Booking:
        """Confirm a booking after Razorpay payment verification.

        Flow:
        1. Validate booking exists and is pending
        2. Get payment record
        3. Verify Razorpay signature
        4. Update payment status to 'captured'
        5. Update booking status to 'confirmed'
        6. Notify both parties
        """
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.status != "pending":
            raise BookingConflictError("Booking has already been processed.")

        payment = crud_payment.get_by_booking_id(db, booking_id)
        if not payment:
            raise ResourceNotFoundError("Payment", booking_id)

        # Verify Razorpay signature
        is_valid = payment_service.verify_payment_signature(
            order_id=payment.gateway_order_id,
            payment_id=payment_id,
            signature=signature,
        )
        if not is_valid:
            raise PaymentVerificationError("Razorpay signature verification failed.")

        # Update payment
        payment.gateway_payment_id = payment_id
        payment.status = "captured"
        payment.captured_at = datetime.now(timezone.utc)
        db.add(payment)

        # Update booking
        booking.status = "confirmed"
        db.add(booking)
        db.commit()
        db.refresh(booking)

        # Notify both parties
        notification_service.send_notification(
            db,
            user_id=booking.attendee_id,
            title="Booking Confirmed!",
            body="Your booking payment was successful and the slot has been reserved.",
            notification_type="booking_confirmed",
            reference_id=booking.id,
            reference_type="booking",
        )
        notification_service.send_notification(
            db,
            user_id=booking.creator_id,
            title="Booking Slot Confirmed",
            body=f"Payment of ₹{booking.total_amount} received. Booking confirmed.",
            notification_type="payment_captured",
            reference_id=booking.id,
            reference_type="booking",
        )

        logger.info("booking_confirmed", booking_id=booking.id)
        return booking

    def complete_booking(
        self, db: Session, booking_id: str, creator_user_id: str
    ) -> Booking:
        """Mark a booking as completed (creator only).

        Flow:
        1. Validate booking exists
        2. Validate creator owns this booking
        3. Validate status is 'confirmed' or 'in_progress'
        4. Update status to 'completed'
        5. Trigger review request notification to attendee
        """
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.creator_id != creator_user_id:
            raise ForbiddenError("Only the assigned creator can complete this booking.")

        if booking.status not in ["confirmed", "in_progress"]:
            raise BookingConflictError(
                f"Cannot complete booking with status: {booking.status}"
            )

        booking.status = "completed"
        db.add(booking)
        db.commit()
        db.refresh(booking)

        # Notify attendee to leave a review
        creator = crud_user.get(db, creator_user_id)
        creator_name = creator.full_name if creator else "Your Creator"
        notification_service.send_notification(
            db,
            user_id=booking.attendee_id,
            title="Booking Completed!",
            body=f"{creator_name} marked your booking as completed. Please leave a review!",
            notification_type="review_requested",
            reference_id=booking.id,
            reference_type="booking",
        )

        logger.info("booking_completed", booking_id=booking.id)
        return booking

    def cancel_booking(
        self, db: Session, booking_id: str, user_id: str, reason: str
    ) -> tuple[Booking, Decimal]:
        """Cancel a booking with refund calculation.

        Refund policy (Rule Set 9):
        - >48h before event: 100% refund
        - 12-48h before event: 50% refund
        - <12h before event: no refund

        Returns:
            Tuple of (cancelled_booking, refund_amount).
        """
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.status not in ["pending", "confirmed"]:
            raise BookingConflictError(
                f"Cannot cancel booking with status: {booking.status}"
            )

        user = crud_user.get(db, user_id)
        if not user:
            raise ResourceNotFoundError("User", user_id)

        # Determine cancellation role
        cancelled_by = "admin"
        if user.role == "attendee" and booking.attendee_id == user_id:
            cancelled_by = "attendee"
        elif user.role == "creator" and booking.creator_id == user_id:
            cancelled_by = "creator"

        # Process refund if payment was captured
        payment = crud_payment.get_by_booking_id(db, booking_id)
        refund_amount = Decimal("0.00")
        if payment and payment.status == "captured":
            refund_amount = payment_service.calculate_refund(booking)
            if refund_amount > 0:
                payment_service.initiate_refund(payment.gateway_payment_id, refund_amount)
                payment.refund_amount = refund_amount
                payment.status = "refunded"
                payment.refunded_at = datetime.now(timezone.utc)
                db.add(payment)

        # Update booking
        booking.status = "cancelled"
        booking.cancelled_by = cancelled_by
        booking.cancellation_reason = reason
        db.add(booking)
        db.commit()
        db.refresh(booking)

        # Notify the other party
        target_notify = booking.creator_id if cancelled_by == "attendee" else booking.attendee_id
        notification_service.send_notification(
            db,
            user_id=target_notify,
            title="Booking Cancelled",
            body=f"Booking cancelled by the {cancelled_by}. Reason: {reason}",
            notification_type="booking_cancelled",
            reference_id=booking.id,
            reference_type="booking",
        )

        logger.info(
            "booking_cancelled",
            booking_id=booking.id,
            cancelled_by=cancelled_by,
            refund=str(refund_amount),
        )
        return booking, refund_amount

    def update_booking(
        self, db: Session, booking_id: str, user_id: str, data: UpdateBookingRequest
    ) -> Booking:
        """Update booking details before confirmation (pending status only)."""
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.attendee_id != user_id:
            raise ForbiddenError("Only the booking attendee can update this booking.")

        if booking.status != "pending":
            raise BookingConflictError("Can only update bookings with pending status.")

        update_data = data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(booking, field, value)

        db.add(booking)
        db.commit()
        db.refresh(booking)
        return booking

    def dispute_booking(
        self, db: Session, booking_id: str, user_id: str, reason: str
    ) -> Booking:
        """Raise a dispute on a completed booking."""
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.attendee_id != user_id and booking.creator_id != user_id:
            raise ForbiddenError("Only booking parties can raise a dispute.")

        if booking.status != "completed":
            raise BookingConflictError("Disputes can only be raised on completed bookings.")

        booking.status = "disputed"
        booking.cancellation_reason = reason  # reuse field for dispute reason
        db.add(booking)
        db.commit()
        db.refresh(booking)

        # Notify admin
        logger.warning("booking_disputed", booking_id=booking.id, user_id=user_id)
        return booking


booking_service = BookingService()
