"""
Capturo — FastAPI Dependency Injection

Provides injectable dependencies for route handlers:
- get_current_user()        — decodes JWT, fetches user from DB
- get_current_active_user() — wraps above, verifies is_active
- require_role(*roles)      — factory for role-based access control

All auth dependencies follow Rule Set 6 contract.
"""

from typing import Generator

from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer
from jose import jwt, JWTError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import UnauthorizedError, ForbiddenError
from app.db.session import get_db
from app.crud.crud_user import crud_user
from app.models.user import User

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> User:
    """Decode JWT access token and return the authenticated User.

    Validates:
    1. Token is decodable and not expired.
    2. Token type is 'access' (not refresh).
    3. User exists in DB and is active.

    Args:
        token: Bearer token extracted by OAuth2PasswordBearer.
        db: SQLAlchemy database session.

    Returns:
        The authenticated User ORM instance.

    Raises:
        UnauthorizedError: If token is invalid, expired, or user not found.
    """
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id: str = payload.get("sub")
        token_type: str = payload.get("type")
        if user_id is None or token_type != "access":
            raise UnauthorizedError("Invalid token")
    except JWTError:
        raise UnauthorizedError("Token is invalid or expired")

    user = crud_user.get(db, id=user_id)
    if user is None or not user.is_active:
        raise UnauthorizedError("User not found or deactivated")
    return user


async def get_current_active_user(
    current_user: User = Depends(get_current_user),
) -> User:
    """Ensure the authenticated user's account is active.

    Args:
        current_user: User from get_current_user dependency.

    Returns:
        The active User ORM instance.

    Raises:
        ForbiddenError: If the account has been deactivated.
    """
    if not current_user.is_active:
        raise ForbiddenError("Account is deactivated")
    return current_user


def require_role(*roles: str):
    """Factory that creates a dependency enforcing role-based access.

    Usage:
        @router.post("/creators/profile")
        async def create_profile(
            current_user: User = Depends(require_role("creator")),
        ): ...

    Args:
        *roles: One or more allowed role strings (e.g., "creator", "admin").

    Returns:
        An async dependency function that validates the user's role.

    Raises:
        ForbiddenError: If the user's role is not in the allowed set.
    """

    async def checker(
        current_user: User = Depends(get_current_active_user),
    ) -> User:
        if current_user.role not in roles:
            raise ForbiddenError(f"This action requires role: {', '.join(roles)}")
        return current_user

    return checker
