"""
Capturo — Authentication Routes

POST /auth/register       — Register new user
POST /auth/login          — Login and receive JWT tokens
POST /auth/refresh        — Refresh access token
POST /auth/logout         — Invalidate refresh token
POST /auth/forgot-password — Send password reset email
POST /auth/reset-password  — Set new password using reset token
POST /auth/verify-email    — Verify email with OTP
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    TokenResponse,
    TokenRefreshResponse,
    RefreshTokenRequest,
    LogoutRequest,
    ForgotPasswordRequest,
    ResetPasswordRequest,
    VerifyEmailRequest,
    AuthResponse,
)
from app.schemas.user import UserResponse
from app.services.auth_service import auth_service

router = APIRouter()


@router.post("/register", response_model=SuccessResponse[UserResponse], status_code=201)
def register(data: RegisterRequest, db: Session = Depends(get_db)):
    """Register a new user account (attendee or creator)."""
    user = auth_service.register_user(db, data)
    return SuccessResponse(
        message="User registered successfully",
        data=UserResponse.model_validate(user),
    )


@router.post("/login", response_model=SuccessResponse[AuthResponse])
def login(data: LoginRequest, db: Session = Depends(get_db)):
    """Login and receive JWT access + refresh tokens."""
    auth_data = auth_service.authenticate_user(db, data)
    token_resp = AuthResponse(
        access_token=auth_data["access_token"],
        refresh_token=auth_data["refresh_token"],
        expires_in=auth_data["expires_in"],
        user=UserResponse.model_validate(auth_data["user"]),
    )
    return SuccessResponse(message="Login successful", data=token_resp)


@router.post("/refresh", response_model=SuccessResponse[TokenRefreshResponse])
def refresh_token(data: RefreshTokenRequest, db: Session = Depends(get_db)):
    """Refresh access token using a valid refresh token."""
    result = auth_service.refresh_token(db, data.refresh_token)
    return SuccessResponse(
        message="Token refreshed successfully",
        data=TokenRefreshResponse(
            access_token=result["access_token"],
            expires_in=result["expires_in"],
        ),
    )


@router.post("/logout", response_model=SuccessResponse[dict])
def logout(
    data: LogoutRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
):
    """Invalidate refresh token (blacklist)."""
    auth_service.logout(db, data.refresh_token)
    return SuccessResponse(message="Logged out successfully", data={})


@router.post("/forgot-password", response_model=SuccessResponse[dict])
def forgot_password(data: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """Send password reset email. Never confirms whether the email exists."""
    auth_service.forgot_password(db, data.email)
    return SuccessResponse(
        message="If an account exists with this email, a reset link has been sent.",
        data={},
    )


@router.post("/reset-password", response_model=SuccessResponse[dict])
def reset_password(data: ResetPasswordRequest, db: Session = Depends(get_db)):
    """Set new password using a valid reset token."""
    auth_service.reset_password(db, data.token, data.new_password)
    return SuccessResponse(message="Password updated successfully", data={})


@router.post("/verify-email", response_model=SuccessResponse[dict])
def verify_email(data: VerifyEmailRequest, db: Session = Depends(get_db)):
    """Verify email address with OTP code."""
    auth_service.verify_email(db, data.email, data.otp)
    return SuccessResponse(message="Email verified successfully", data={"is_verified": True})
