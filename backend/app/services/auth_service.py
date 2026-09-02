"""
Capturo — Authentication Service

Business logic for user registration, login, token refresh, logout,
password reset, and email verification. This is the ONLY layer that
touches auth-related business rules (Rule Set 1).

Exceptions raised are typed (Rule Set 12):
- DuplicateResourceError for duplicate email/phone
- UnauthorizedError for invalid credentials
- AccountDeactivatedError for deactivated accounts
- InvalidTokenError for bad/expired tokens
"""

from datetime import datetime, timezone, timedelta
import random
import string
from typing import Any, Dict, Optional

from jose import JWTError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import (
    DuplicateResourceError,
    UnauthorizedError,
    AccountDeactivatedError,
    InvalidTokenError,
    ResourceNotFoundError,
)
from app.core.security import (
    get_password_hash,
    verify_password,
    create_access_token,
    create_refresh_token,
    decode_token,
    create_email_verification_token,
    create_password_reset_token,
)
from app.crud.crud_user import crud_user
from app.crud.crud_creator import crud_creator
from app.models.user import User
from app.models.creator import CreatorProfile
from app.models.email_otp import EmailOTP
from app.schemas.auth import RegisterRequest, LoginRequest
from app.services.email_service import email_service
from app.utils.logger import logger


class AuthService:
    """Authentication and identity management business logic."""

    def register_user(self, db: Session, data: RegisterRequest) -> User:
        """Register a new user account.

        1. Check email uniqueness → DuplicateResourceError
        2. Check phone uniqueness (if provided) → DuplicateResourceError
        3. Hash password, create user record
        4. If role='creator', auto-create empty CreatorProfile
        5. Send verification email

        Returns:
            The created User ORM instance.
        """
        # Check email uniqueness
        existing = crud_user.get_by_email(db, data.email)
        if existing:
            raise DuplicateResourceError("email", data.email)

        # Check phone uniqueness
        if data.phone:
            existing_phone = crud_user.get_by_phone(db, data.phone)
            if existing_phone:
                raise DuplicateResourceError("phone", data.phone)

        # Create user
        user_data = data.model_dump()
        user_data["hashed_password"] = get_password_hash(user_data.pop("password"))

        db_user = User(**user_data)
        db.add(db_user)
        db.commit()
        db.refresh(db_user)

        # Auto-create creator profile if role is creator
        if db_user.role == "creator":
            creator_profile = CreatorProfile(user_id=db_user.id)
            db.add(creator_profile)
            db.commit()

        # Send verification email (non-blocking)
        try:
            otp_code = "".join(random.choices(string.digits, k=6))
            expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)
            db_otp = EmailOTP(
                user_id=db_user.id,
                otp_code=otp_code,
                otp_type="verify_email",
                is_used=False,
                expires_at=expires_at
            )
            db.add(db_otp)
            db.commit()

            email_service.send_email(
                to_email=db_user.email,
                subject="Capturo — Verify Your Email",
                body=f"Your verification code: {otp_code}\n\nThis code expires in 15 minutes.",
            )
        except Exception as e:
            logger.warning("verification_email_failed", user_id=db_user.id, error=str(e))

        logger.info("user_registered", user_id=db_user.id, role=db_user.role)
        return db_user

    def authenticate_user(self, db: Session, data: LoginRequest) -> Dict[str, Any]:
        """Authenticate user with email/password and return token pair.

        NEVER distinguish between 'email not found' and 'wrong password'
        to prevent user enumeration attacks.

        Returns:
            Dict with access_token, refresh_token, expires_in, and user.
        """
        user = crud_user.get_by_email(db, data.email)
        if not user or not verify_password(data.password, user.hashed_password):
            raise UnauthorizedError("Incorrect email or password")

        if not user.is_active:
            raise AccountDeactivatedError("Account is deactivated")

        # Update last login timestamp
        user.last_login_at = datetime.now(timezone.utc)
        db.add(user)
        db.commit()

        access_token = create_access_token(user.id, user.role)
        refresh_token = create_refresh_token(user.id, user.role)

        logger.info("user_login", user_id=user.id)
        return {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            "user": user,
        }

    def refresh_token(self, db: Session, refresh_token_str: str) -> Dict[str, Any]:
        """Issue a new access token using a valid refresh token.

        Validates:
        1. Token is decodable and not expired
        2. Token type is 'refresh'
        3. User still exists and is active

        Returns:
            Dict with new access_token and expires_in.
        """
        try:
            payload = decode_token(refresh_token_str)
            user_id = payload.get("sub")
            token_type = payload.get("type")
            if not user_id or token_type != "refresh":
                raise InvalidTokenError("Invalid refresh token")
        except JWTError:
            raise InvalidTokenError("Refresh token is invalid or expired")

        user = crud_user.get(db, user_id)
        if not user or not user.is_active:
            raise InvalidTokenError("User not found or deactivated")

        new_access_token = create_access_token(user.id, user.role)
        return {
            "access_token": new_access_token,
            "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        }

    def logout(self, db: Session, refresh_token_str: str) -> None:
        """Invalidate a refresh token (blacklist).

        Note: In a production system, store blacklisted tokens in a DB table
        or Redis set with TTL matching the token's remaining lifetime.
        For now, this validates and logs the logout event.
        """
        try:
            payload = decode_token(refresh_token_str)
            user_id = payload.get("sub")
            logger.info("user_logout", user_id=user_id)
        except JWTError:
            raise InvalidTokenError("Invalid refresh token")

    def forgot_password(self, db: Session, email: str) -> None:
        """Send password reset email."""
        user = crud_user.get_by_email(db, email)
        if user:
            otp_code = "".join(random.choices(string.digits, k=6))
            expires_at = datetime.now(timezone.utc) + timedelta(minutes=30)
            db_otp = EmailOTP(
                user_id=user.id,
                otp_code=otp_code,
                otp_type="reset_pwd",
                is_used=False,
                expires_at=expires_at
            )
            db.add(db_otp)
            db.commit()

            try:
                email_service.send_email(
                    to_email=email,
                    subject="Capturo — Reset Your Password",
                    body=f"Your password reset code: {otp_code}\n\nThis code expires in 30 minutes.",
                )
                logger.info("password_reset_sent", user_id=user.id)
            except Exception as e:
                logger.error("password_reset_email_failed", error=str(e))

    def reset_password(self, db: Session, token: str, new_password: str) -> None:
        """Reset user password using a valid reset token or 6-digit OTP."""
        # 1. Try to treat token as a 6-digit OTP code first if it matches numeric OTP structure
        if len(token) == 6 and token.isdigit():
            now = datetime.now(timezone.utc)
            db_otp = db.query(EmailOTP).filter(
                EmailOTP.otp_code == token,
                EmailOTP.otp_type == "reset_pwd",
                EmailOTP.is_used == False,
                EmailOTP.expires_at > now
            ).order_by(EmailOTP.created_at.desc()).first()

            if not db_otp:
                raise InvalidTokenError("Invalid or expired password reset code")

            user_id = db_otp.user_id
            db_otp.is_used = True
            db.add(db_otp)
        else:
            # Fallback to JWT token validation
            try:
                payload = decode_token(token)
                user_id = payload.get("sub")
                token_type = payload.get("type")
                if not user_id or token_type != "password_reset":
                    raise InvalidTokenError("Invalid reset token")
            except JWTError:
                raise InvalidTokenError("Reset token is invalid or expired")

        user = crud_user.get(db, user_id)
        if not user:
            raise ResourceNotFoundError("User", user_id)

        user.hashed_password = get_password_hash(new_password)
        db.add(user)
        db.commit()
        logger.info("password_reset_completed", user_id=user.id)

    def verify_email(self, db: Session, email: str, otp: str) -> None:
        """Verify user email using OTP code.

        Validates the OTP code from the email_otps table.
        """
        user = crud_user.get_by_email(db, email)
        if not user:
            raise ResourceNotFoundError("User", email)

        now = datetime.now(timezone.utc)
        db_otp = db.query(EmailOTP).filter(
            EmailOTP.user_id == user.id,
            EmailOTP.otp_code == otp,
            EmailOTP.otp_type == "verify_email",
            EmailOTP.is_used == False,
            EmailOTP.expires_at > now
        ).order_by(EmailOTP.created_at.desc()).first()

        if not db_otp:
            raise InvalidTokenError("Invalid or expired email verification code")

        db_otp.is_used = True
        user.is_verified = True
        db.add(db_otp)
        db.add(user)
        db.commit()
        logger.info("email_verified", user_id=user.id)


auth_service = AuthService()
