"""
Capturo — Booking CRUD Operations

Database operations for the Booking model including availability checks,
status transitions, and filtered queries for attendee/creator views.
"""

from typing import List, Optional
from datetime import date, time, datetime, timedelta, timezone

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.booking import Booking
from app.schemas.booking import CreateBookingRequest, UpdateBookingRequest


class CRUDBooking(CRUDBase[Booking, CreateBookingRequest, UpdateBookingRequest]):
    """CRUD operations for the Booking model."""

    def get_by_attendee(
        self, db: Session, attendee_id: str, status: Optional[str] = None, skip: int = 0, limit: int = 20
    ) -> tuple[List[Booking], int]:
        """Get paginated bookings for an attendee, ordered by created_at DESC."""
        query = db.query(self.model).filter(self.model.attendee_id == attendee_id)
        if status:
            query = query.filter(self.model.status == status)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def get_by_creator(
        self, db: Session, creator_id: str, status: Optional[str] = None, skip: int = 0, limit: int = 20
    ) -> tuple[List[Booking], int]:
        """Get paginated bookings for a creator, ordered by event_date ASC."""
        query = db.query(self.model).filter(self.model.creator_id == creator_id)
        if status:
            query = query.filter(self.model.status == status)
        total = query.count()
        items = query.order_by(self.model.event_date.asc()).offset(skip).limit(limit).all()
        return items, total

    def check_availability(
        self, db: Session, creator_id: str, event_date: date, start_time: time, duration_hours: float
    ) -> bool:
        """Check if a creator is available at a specific date/time slot.

        Uses time overlap logic: two bookings overlap when
        start1 < end2 AND start2 < end1 (strict inequality means
        a booking ending exactly when a new one starts is NOT a conflict).
        """
        dummy_date = date(2000, 1, 1)
        req_start_dt = datetime.combine(dummy_date, start_time)
        req_end_dt = req_start_dt + timedelta(hours=float(duration_hours))

        # Get all non-cancelled bookings for this creator on this date
        all_bookings = db.query(self.model).filter(
            self.model.creator_id == creator_id,
            self.model.event_date == event_date,
            self.model.status.notin_(["cancelled"]),
        ).all()

        now_dt = datetime.now()
        active_bookings = []
        for booking in all_bookings:
            if booking.status == "pending":
                created_at = booking.created_at
                if created_at.tzinfo is not None:
                    now_tz = now_dt.astimezone(created_at.tzinfo)
                    diff = now_tz - created_at
                else:
                    diff = now_dt - created_at
                if diff.total_seconds() > 900:  # 15 minutes
                    continue
            active_bookings.append(booking)

        for booking in active_bookings:
            b_start_dt = datetime.combine(dummy_date, booking.start_time)
            b_end_dt = b_start_dt + timedelta(hours=float(booking.duration_hours))
            if req_start_dt < b_end_dt and b_start_dt < req_end_dt:
                return False
        return True

    def get_for_date(self, db: Session, creator_id: str, target_date: date) -> List[Booking]:
        """Get confirmed/in_progress bookings for a creator on a specific date (Today's Schedule)."""
        return db.query(self.model).filter(
            self.model.creator_id == creator_id,
            self.model.event_date == target_date,
            self.model.status.in_(["confirmed", "in_progress"]),
        ).order_by(self.model.start_time.asc()).all()

    def update_status(self, db: Session, booking_id: str, new_status: str, **extra_fields) -> Optional[Booking]:
        """Atomically update booking status and any extra fields."""
        booking = self.get(db, booking_id)
        if booking:
            booking.status = new_status
            for field, value in extra_fields.items():
                setattr(booking, field, value)
            db.add(booking)
            db.commit()
            db.refresh(booking)
        return booking

    def get_upcoming(self, db: Session, creator_id: str, days_ahead: int = 7) -> List[Booking]:
        """Get confirmed bookings from today up to days_ahead days."""
        today = date.today()
        end_date = today + timedelta(days=days_ahead)
        return db.query(self.model).filter(
            self.model.creator_id == creator_id,
            self.model.event_date.between(today, end_date),
            self.model.status == "confirmed",
        ).order_by(self.model.event_date.asc()).all()

    def get_all_with_filters(
        self,
        db: Session,
        *,
        status: Optional[str] = None,
        date_from: Optional[date] = None,
        date_to: Optional[date] = None,
        skip: int = 0,
        limit: int = 20,
    ) -> tuple[List[Booking], int]:
        """Get paginated bookings with filters (admin use)."""
        query = db.query(self.model)
        if status:
            query = query.filter(self.model.status == status)
        if date_from:
            query = query.filter(self.model.event_date >= date_from)
        if date_to:
            query = query.filter(self.model.event_date <= date_to)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def hard_delete(self, db: Session, booking_id: str) -> bool:
        """Permanently delete a booking record (admin only)."""
        booking = self.get(db, booking_id)
        if booking:
            db.delete(booking)
            db.commit()
            return True
        return False


crud_booking = CRUDBooking(Booking)
