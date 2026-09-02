"""
Capturo — Booking ORM Model

Represents a booking between an attendee and a creator with full
status state machine: pending → confirmed → in_progress → completed.
Cancellation and dispute flows branch from confirmed state.
"""

from uuid import uuid4

from sqlalchemy import Column, String, ForeignKey, Enum, Date, Time, Numeric, Text, DateTime, func, Index
from sqlalchemy.orm import relationship

from app.db.session import Base

class Booking(Base):
    __tablename__ = "bookings"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    attendee_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    creator_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    event_type = Column(
        Enum("wedding", "birthday", "corporate", "graduation", "party", "other", native_enum=False),
        nullable=False
    )
    location = Column(String(500), nullable=False)
    event_date = Column(Date, nullable=False)
    start_time = Column(Time, nullable=False)
    duration_hours = Column(Numeric(4, 1), nullable=False)
    total_amount = Column(Numeric(12, 2), nullable=False)
    status = Column(
        Enum("pending", "confirmed", "in_progress", "completed", "cancelled", "disputed", native_enum=False),
        nullable=False,
        default="pending"
    )
    special_notes = Column(Text, nullable=True)
    invoice_url = Column(String(500), nullable=True)
    cancelled_by = Column(Enum("attendee", "creator", "admin", native_enum=False), nullable=True)
    cancellation_reason = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)



    # Relationships
    attendee = relationship("User", foreign_keys=[attendee_id], back_populates="bookings_as_attendee")
    creator = relationship("User", foreign_keys=[creator_id], back_populates="bookings_as_creator")
    payment = relationship("Payment", back_populates="booking", uselist=False, cascade="all, delete-orphan")
    messages = relationship("Message", back_populates="booking", cascade="all, delete-orphan")
    review = relationship("Review", back_populates="booking", uselist=False, cascade="all, delete-orphan")
    gallery_items = relationship("GalleryItem", back_populates="booking", cascade="all, delete-orphan")

    __table_args__ = (
        Index("idx_bookings_attendee_status", "attendee_id", "status"),
        Index("idx_bookings_creator_status", "creator_id", "status"),
        Index("idx_bookings_event_date", "event_date"),
        Index("idx_bookings_creator_date", "creator_id", "event_date"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
