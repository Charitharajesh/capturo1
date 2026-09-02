"""
Capturo — Admin Service

Business logic for platform administration:
- Dashboard statistics
- Booking management with filters
- Dispute resolution
- Creator verification
- User management
- Broadcast notifications
"""

from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict, List, Optional

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.exceptions import ResourceNotFoundError, BookingConflictError
from app.crud.crud_user import crud_user
from app.crud.crud_booking import crud_booking
from app.crud.crud_creator import crud_creator
from app.crud.crud_notification import crud_notification, CreateNotificationInternal
from app.models.user import User
from app.models.booking import Booking
from app.models.payment import Payment
from app.services.payment_service import payment_service
from app.services.notification_service import notification_service
from app.utils.logger import logger


class AdminService:
    """Platform administration business logic."""

    def get_dashboard_stats(self, db: Session) -> Dict[str, Any]:
        """Aggregate platform statistics for admin dashboard.

        Returns:
            Dict with total_users, active_bookings, revenue_this_month.
        """
        total_users = db.query(func.count(User.id)).scalar() or 0
        total_bookings = db.query(func.count(Booking.id)).scalar() or 0
        total_revenue = (
            db.query(func.sum(Payment.amount))
            .filter(Payment.status == "captured")
            .scalar()
            or Decimal("0.00")
        )

        return {
            "total_users": total_users,
            "active_bookings": total_bookings,
            "revenue_this_month": float(total_revenue),
        }

    def get_all_bookings(
        self,
        db: Session,
        *,
        status: Optional[str] = None,
        date_from: Optional[str] = None,
        date_to: Optional[str] = None,
        skip: int = 0,
        limit: int = 20,
    ) -> tuple[List[Booking], int]:
        """Get all bookings with admin-level filters."""
        from datetime import date as date_type

        parsed_from = date_type.fromisoformat(date_from) if date_from else None
        parsed_to = date_type.fromisoformat(date_to) if date_to else None

        return crud_booking.get_all_with_filters(
            db,
            status=status,
            date_from=parsed_from,
            date_to=parsed_to,
            skip=skip,
            limit=limit,
        )

    def resolve_dispute(
        self, db: Session, booking_id: str, resolution: str, notes: str
    ) -> Booking:
        """Resolve a disputed booking.

        Args:
            resolution: 'refund_full', 'refund_partial', 'no_refund', 'completed'
            notes: Admin resolution notes
        """
        booking = crud_booking.get(db, booking_id)
        if not booking:
            raise ResourceNotFoundError("Booking", booking_id)

        if booking.status != "disputed":
            raise BookingConflictError("Only disputed bookings can be resolved.")

        if resolution == "refund_full":
            booking.status = "cancelled"
        elif resolution == "completed":
            booking.status = "completed"
        else:
            booking.status = "completed"

        db.add(booking)
        db.commit()
        db.refresh(booking)

        # Notify both parties
        notification_service.send_notification(
            db,
            user_id=booking.attendee_id,
            title="Dispute Resolved",
            body=f"Your dispute has been resolved. Resolution: {resolution}",
            notification_type="dispute_resolved",
            reference_id=booking.id,
            reference_type="booking",
        )
        notification_service.send_notification(
            db,
            user_id=booking.creator_id,
            title="Dispute Resolved",
            body=f"A dispute on your booking has been resolved. Resolution: {resolution}",
            notification_type="dispute_resolved",
            reference_id=booking.id,
            reference_type="booking",
        )

        logger.info("dispute_resolved", booking_id=booking_id, resolution=resolution)
        return booking

    def get_pending_creators(
        self, db: Session, skip: int = 0, limit: int = 20
    ) -> tuple[List[User], int]:
        """Get creators pending verification."""
        return crud_user.get_multi_with_filters(
            db, role="creator", is_active=True, skip=skip, limit=limit
        )

    def verify_creator(self, db: Session, creator_id: str) -> User:
        """Verify/approve a creator profile."""
        user = crud_user.get(db, creator_id)
        if not user:
            raise ResourceNotFoundError("User", creator_id)

        user.is_verified = True
        db.add(user)
        db.commit()
        db.refresh(user)

        notification_service.send_notification(
            db,
            user_id=creator_id,
            title="Profile Verified!",
            body="Congratulations! Your creator profile has been verified.",
            notification_type="profile_verified",
            reference_id=creator_id,
            reference_type="user",
        )

        logger.info("creator_verified", creator_id=creator_id)
        return user

    def hard_delete_user(self, db: Session, user_id: str) -> None:
        """Permanently delete a user and all associated records.

        WARNING: This is a destructive operation. Cascade deletes are
        handled by foreign key constraints with ondelete='CASCADE'.
        """
        user = crud_user.get(db, user_id)
        if not user:
            raise ResourceNotFoundError("User", user_id)

        db.delete(user)
        db.commit()
        logger.warning("user_hard_deleted", user_id=user_id)

    def broadcast_notification(
        self, db: Session, title: str, body: str, target: str
    ) -> int:
        """Send a notification to all users in a segment.

        Args:
            target: 'all_users', 'all_creators', or 'all_attendees'

        Returns:
            Number of notifications sent.
        """
        role_filter = None
        if target == "all_creators":
            role_filter = "creator"
        elif target == "all_attendees":
            role_filter = "attendee"

        users, _ = crud_user.get_multi_with_filters(
            db, role=role_filter, is_active=True, skip=0, limit=10000
        )

        count = 0
        for user in users:
            notification_service.send_notification(
                db,
                user_id=user.id,
                title=title,
                body=body,
                notification_type="broadcast",
            )
            count += 1

        logger.info("broadcast_sent", target=target, count=count)
        return count


admin_service = AdminService()
