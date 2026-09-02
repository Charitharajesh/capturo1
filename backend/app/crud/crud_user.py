"""
Capturo — User CRUD Operations

Database operations for the User model.
"""

from typing import List, Optional

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.user import User
from app.schemas.auth import RegisterRequest
from app.schemas.user import UpdateUserRequest


class CRUDUser(CRUDBase[User, RegisterRequest, UpdateUserRequest]):
    """CRUD operations for the User model."""

    def get_by_email(self, db: Session, email: str) -> Optional[User]:
        """Find user by email address (case-insensitive in MySQL)."""
        return db.query(self.model).filter(self.model.email == email).first()

    def get_by_phone(self, db: Session, phone: str) -> Optional[User]:
        """Find user by phone number."""
        return db.query(self.model).filter(self.model.phone == phone).first()

    def get_multi_with_filters(
        self,
        db: Session,
        *,
        role: Optional[str] = None,
        is_active: Optional[bool] = None,
        skip: int = 0,
        limit: int = 20,
    ) -> tuple[List[User], int]:
        """Get paginated user list with optional role/status filters (admin)."""
        query = db.query(self.model)
        if role:
            query = query.filter(self.model.role == role)
        if is_active is not None:
            query = query.filter(self.model.is_active == is_active)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

    def deactivate(self, db: Session, user_id: str) -> Optional[User]:
        """Soft-delete a user by setting is_active=False."""
        user = self.get(db, user_id)
        if user:
            user.is_active = False
            db.add(user)
            db.commit()
            db.refresh(user)
        return user


crud_user = CRUDUser(User)
