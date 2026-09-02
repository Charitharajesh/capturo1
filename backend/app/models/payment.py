from uuid import uuid4
from sqlalchemy import Column, String, ForeignKey, Numeric, Enum, DateTime, func, Index
from sqlalchemy.orm import relationship
from app.db.session import Base

class Payment(Base):
    __tablename__ = "payments"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    booking_id = Column(String(36), ForeignKey("bookings.id", ondelete="CASCADE"), nullable=False, unique=True)
    payer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    amount = Column(Numeric(12, 2), nullable=False)
    refund_amount = Column(Numeric(12, 2), nullable=False, default=0.00)
    currency = Column(String(3), nullable=False, default="INR")
    gateway = Column(Enum("razorpay", "stripe", "cash", native_enum=False), nullable=False, default="razorpay")
    gateway_order_id = Column(String(100), nullable=True)
    gateway_payment_id = Column(String(100), nullable=True, unique=True)
    status = Column(
        Enum("pending", "authorized", "captured", "failed", "refunded", native_enum=False),
        nullable=False,
        default="pending"
    )
    captured_at = Column(DateTime(timezone=True), nullable=True)
    refunded_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    booking = relationship("Booking", back_populates="payment")
    payer = relationship("User", foreign_keys=[payer_id])

    __table_args__ = (
        Index("idx_payments_payer_status", "payer_id", "status"),
        Index("idx_payments_status_created", "status", "created_at"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
