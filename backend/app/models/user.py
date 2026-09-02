from uuid import uuid4
from sqlalchemy import Column, String, Enum, Boolean, DateTime, func, Index
from sqlalchemy.orm import relationship
from app.db.session import Base

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    full_name = Column(String(150), nullable=False)
    email = Column(String(255), unique=True, nullable=False, index=True)
    phone = Column(String(20), nullable=True)
    hashed_password = Column(String(255), nullable=False)
    role = Column(Enum("attendee", "creator", "admin", native_enum=False), nullable=False, default="attendee")
    profile_pic_url = Column(String(500), nullable=True)
    is_active = Column(Boolean, nullable=False, default=True)
    is_verified = Column(Boolean, nullable=False, default=False)
    fcm_token = Column(String(500), nullable=True)
    last_login_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # Relationships
    creator_profile = relationship("CreatorProfile", back_populates="user", uselist=False, cascade="all, delete-orphan")
    bookings_as_attendee = relationship(
        "Booking",
        back_populates="attendee",
        foreign_keys="[Booking.attendee_id]",
        cascade="all, delete-orphan"
    )
    bookings_as_creator = relationship(
        "Booking",
        back_populates="creator",
        foreign_keys="[Booking.creator_id]",
        cascade="all, delete-orphan"
    )
    messages_sent = relationship(
        "Message",
        back_populates="sender",
        foreign_keys="[Message.sender_id]",
        cascade="all, delete-orphan"
    )
    messages_received = relationship(
        "Message",
        back_populates="receiver",
        foreign_keys="[Message.receiver_id]",
        cascade="all, delete-orphan"
    )
    reviews_given = relationship(
        "Review",
        back_populates="reviewer",
        foreign_keys="[Review.reviewer_id]",
        cascade="all, delete-orphan"
    )
    reviews_received = relationship(
        "Review",
        back_populates="creator",
        foreign_keys="[Review.creator_id]",
        cascade="all, delete-orphan"
    )
    notifications = relationship("Notification", back_populates="user", cascade="all, delete-orphan")
    gallery_items = relationship("GalleryItem", back_populates="creator", cascade="all, delete-orphan")
    refresh_tokens = relationship("RefreshToken", back_populates="user", cascade="all, delete-orphan")
    email_otps = relationship("EmailOTP", back_populates="user", cascade="all, delete-orphan")
    ai_usage_logs = relationship("AIUsageLog", back_populates="user", cascade="all, delete-orphan")

    __table_args__ = (
        Index("idx_users_role", "role"),
        Index("idx_users_active_role", "is_active", "role"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
