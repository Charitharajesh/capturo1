from uuid import uuid4
from sqlalchemy import Column, String, ForeignKey, Enum, Boolean, DateTime, Text, func, Index
from sqlalchemy.orm import relationship
from app.db.session import Base

class Notification(Base):
    __tablename__ = "notifications"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    title = Column(String(200), nullable=False)
    body = Column(Text, nullable=False)
    notification_type = Column(
        Enum(
            "booking_confirmed",
            "booking_cancelled",
            "new_message",
            "payment_captured",
            "review_requested",
            "creator_accepted",
            "upload_ready",
            native_enum=False
        ),
        nullable=False
    )
    reference_id = Column(String(36), nullable=True)
    reference_type = Column(String(50), nullable=True)
    is_read = Column(Boolean, nullable=False, default=False)
    read_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    user = relationship("User", back_populates="notifications")

    __table_args__ = (
        Index("idx_notifications_user_read", "user_id", "is_read", "created_at"),
        Index("idx_notifications_type", "notification_type"),
        Index("idx_notifications_reference", "reference_id", "reference_type"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
