from uuid import uuid4
from sqlalchemy import Column, String, Boolean, DateTime, func, ForeignKey, Enum
from sqlalchemy.orm import relationship
from app.db.session import Base

class EmailOTP(Base):
    __tablename__ = "email_otps"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    otp_code = Column(String(255), nullable=False)
    otp_type = Column(Enum("verify_email", "reset_pwd", native_enum=False), nullable=False)
    is_used = Column(Boolean, nullable=False, default=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    user = relationship("User", back_populates="email_otps")
