"""
Capturo — Creator Profile ORM Model

Represents a creator's professional profile with specializations,
pricing, location, ratings, and availability status.
"""

from uuid import uuid4

from sqlalchemy import Column, String, ForeignKey, JSON, Numeric, Integer, Enum, DateTime, Boolean, func, Index
from sqlalchemy.orm import relationship

from app.db.session import Base

class CreatorProfile(Base):
    __tablename__ = "creator_profiles"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True)
    specializations = Column(JSON, nullable=False, default=list)  # List of strings e.g. ["wedding", "portrait"]
    hourly_rate = Column(Numeric(10, 2), nullable=False, default=999.00)
    minimum_hours = Column(Integer, nullable=False, default=2)
    bio = Column(String(1000), nullable=True)
    years_experience = Column(Integer, nullable=True)
    equipment = Column(JSON, nullable=True)  # List of equipment strings
    availability_status = Column(Enum("available", "busy", "offline", native_enum=False), nullable=False, default="available")
    avg_rating = Column(Numeric(3, 2), nullable=False, default=0.00)
    total_reviews = Column(Integer, nullable=False, default=0)
    total_bookings = Column(Integer, nullable=False, default=0)
    on_time_rate = Column(Numeric(5, 2), nullable=False, default=100.00)
    latitude = Column(Numeric(10, 8), nullable=True)
    longitude = Column(Numeric(11, 8), nullable=True)
    service_radius_km = Column(Integer, nullable=False, default=10)
    is_featured = Column(Boolean, nullable=False, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # Relationships
    user = relationship("User", back_populates="creator_profile")

    __table_args__ = (
        Index("idx_creators_status_rating", "availability_status", "avg_rating"),
        Index("idx_creators_location", "latitude", "longitude"),
        Index("idx_creators_featured", "is_featured"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )


class CreatorFollower(Base):
    __tablename__ = "creator_followers"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    follower_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    creator_id = Column(String(36), ForeignKey("creator_profiles.id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    __table_args__ = (
        Index("idx_followers_lookup", "follower_id", "creator_id"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
