from uuid import uuid4
from sqlalchemy import Column, String, ForeignKey, Enum, BigInteger, Boolean, Integer, Text, DateTime, func, Index
from sqlalchemy.orm import relationship
from app.db.session import Base

class GalleryItem(Base):
    __tablename__ = "gallery_items"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    creator_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    booking_id = Column(String(36), ForeignKey("bookings.id", ondelete="SET NULL"), nullable=True)
    file_url = Column(String(500), nullable=False)
    thumbnail_url = Column(String(500), nullable=True)
    file_type = Column(Enum("photo", "video", native_enum=False), nullable=False)
    file_size_bytes = Column(BigInteger, nullable=False)
    title = Column(String(200), nullable=True)
    description = Column(Text, nullable=True)
    is_portfolio = Column(Boolean, nullable=False, default=False)
    is_client_delivery = Column(Boolean, nullable=False, default=False)
    views_count = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    creator = relationship("User", back_populates="gallery_items")
    booking = relationship("Booking", back_populates="gallery_items")

    __table_args__ = (
        Index("idx_gallery_creator_portfolio", "creator_id", "is_portfolio", "created_at"),
        Index("idx_gallery_booking_delivery", "booking_id", "is_client_delivery"),
        Index("idx_gallery_file_type", "file_type"),
        {
            "mysql_engine": "InnoDB",
            "mysql_charset": "utf8mb4",
            "mysql_collate": "utf8mb4_unicode_ci"
        }
    )
