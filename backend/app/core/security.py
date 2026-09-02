"""
Capturo — JWT & Password Security Utilities

Provides:
- bcrypt password hashing and verification (passlib)
- JWT access token creation (60 min TTL)
- JWT refresh token creation (30 day TTL)
- Token decoding with type validation
- Email verification and password reset token generation

All datetime operations use timezone-aware UTC (Rule Set 4).
Token payload structure follows Rule Set 6.
"""

from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional, Union

from jose import jwt, JWTError
import bcrypt
from app.core.config import settings

# ── Password Hashing ────────────────────────────────────────────────


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plain-text password against its bcrypt hash.

    Args:
        plain_password: The user-supplied password (NEVER logged).
        hashed_password: The stored bcrypt hash from the database.

    Returns:
        True if the password matches, False otherwise.
    """
    try:
        return bcrypt.checkpw(
            plain_password.encode("utf-8"),
            hashed_password.encode("utf-8")
        )
    except Exception:
        return False


def get_password_hash(password: str) -> str:
    """Hash a plain-text password using bcrypt.

    Args:
        password: The plain-text password (NEVER logged).

    Returns:
        bcrypt hash string suitable for database storage.
    """
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode("utf-8"), salt)
    return hashed.decode("utf-8")


# ── JWT Token Creation ──────────────────────────────────────────────

def create_access_token(
    subject: Union[str, Any],
    role: str,
    expires_delta: Optional[timedelta] = None,
) -> str:
    """Create a short-lived JWT access token.

    Args:
        subject: User ID (stored as 'sub' claim).
        role: User role ('attendee', 'creator', 'admin').
        expires_delta: Custom expiry. Defaults to ACCESS_TOKEN_EXPIRE_MINUTES.

    Returns:
        Encoded JWT string.
    """
    now = datetime.now(timezone.utc)
    expire = now + (expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode = {
        "sub": str(subject),
        "role": role,
        "type": "access",
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def create_refresh_token(
    subject: Union[str, Any],
    role: str,
    expires_delta: Optional[timedelta] = None,
) -> str:
    """Create a long-lived JWT refresh token.

    Args:
        subject: User ID (stored as 'sub' claim).
        role: User role.
        expires_delta: Custom expiry. Defaults to REFRESH_TOKEN_EXPIRE_DAYS.

    Returns:
        Encoded JWT string.
    """
    now = datetime.now(timezone.utc)
    expire = now + (expires_delta or timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS))
    to_encode = {
        "sub": str(subject),
        "role": role,
        "type": "refresh",
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def decode_token(token: str) -> Dict[str, Any]:
    """Decode and validate a JWT token.

    Args:
        token: The raw JWT string.

    Returns:
        Decoded payload dict with keys: sub, role, type, exp, iat.

    Raises:
        JWTError: If the token is malformed, expired, or has an invalid signature.
    """
    return jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])


def create_email_verification_token(user_id: str) -> str:
    """Create a short-lived token for email verification (15 min TTL).

    Args:
        user_id: The user ID to encode.

    Returns:
        Encoded JWT string with type='email_verify'.
    """
    now = datetime.now(timezone.utc)
    expire = now + timedelta(minutes=15)
    to_encode = {
        "sub": str(user_id),
        "type": "email_verify",
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def create_password_reset_token(user_id: str) -> str:
    """Create a short-lived token for password reset (30 min TTL).

    Args:
        user_id: The user ID to encode.

    Returns:
        Encoded JWT string with type='password_reset'.
    """
    now = datetime.now(timezone.utc)
    expire = now + timedelta(minutes=30)
    to_encode = {
        "sub": str(user_id),
        "type": "password_reset",
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
