from uuid import uuid4
from sqlalchemy import Column, String, ForeignKey, Enum, Boolean, DateTime, Text, func, Index
from sqlalchemy.orm import relationship
from app.db.session import Base

class Message(Base):
    __tablename__ = "messages"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    booking_id = Column(String(36), ForeignKey("bookings.id", ondelete="CASCADE"), nullable=False)
    sender_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    receiver_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    content = Column(Text, nullable=True)
    message_type = Column(Enum("text", "image", "file", "system", native_enum=False), nullable=False, default="text")
    media_url = Column(String(500), nullable=True)
    is_read = Column(Boolean, nullable=False, default=False)
    read_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    booking = relationship("Booking", back_populates="messages")
    sender = relationship("User", foreign_keys=[sender_id], back_populates="messages_sent")
    receiver = relationship("User", foreign_keys=[receiver_id], back_populates="messages_received")

    __table_args__ = (
        Index("idx_messages_booking_created", "booking_id", "created_at"),
        Index("idx_messages_receiver_read", "receiver_id", "is_read"),
        Index("idx_messages_sender", "sender_id"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
