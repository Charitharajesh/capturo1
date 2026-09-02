"""
Capturo — Authentication Schemas

Request/response schemas for registration, login, token refresh,
logout, password reset, and email verification flows.

Naming convention (Rule Set 5):
- Request schemas: RegisterRequest, LoginRequest, etc.
- Response schemas: AuthResponse, TokenResponse, etc.
"""

from typing import Optional

from pydantic import BaseModel, EmailStr, Field

from app.schemas.user import UserResponse


# ── Registration ─────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    """New user registration payload."""

    full_name: str = Field(..., min_length=2, max_length=150)
    email: EmailStr
    password: str = Field(..., min_length=8, description="Minimum 8 characters")
    role: str = Field("attendee", pattern="^(attendee|creator)$")
    phone: Optional[str] = Field(None, max_length=20)


# ── Login ────────────────────────────────────────────────────────────

class LoginRequest(BaseModel):
    """User login payload. Never distinguish 'email not found' vs 'wrong password'."""

    email: EmailStr
    password: str


# ── Token Responses ──────────────────────────────────────────────────

class TokenResponse(BaseModel):
    """JWT token pair returned on login."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int


class TokenRefreshResponse(BaseModel):
    """New access token returned on refresh."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int


class AuthResponse(BaseModel):
    """Full auth response containing tokens and user profile."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserResponse


# ── Token Payload (internal) ─────────────────────────────────────────

class TokenPayload(BaseModel):
    """Decoded JWT payload structure."""

    sub: str
    role: str
    type: str
    exp: int
    iat: int


# ── Refresh / Logout ────────────────────────────────────────────────

class RefreshTokenRequest(BaseModel):
    """Refresh token submission."""

    refresh_token: str


class LogoutRequest(BaseModel):
    """Logout payload — the refresh token to blacklist."""

    refresh_token: str


# ── Password Reset ──────────────────────────────────────────────────

class ForgotPasswordRequest(BaseModel):
    """Forgot password — triggers reset email.
    NEVER confirm whether the email exists in the system.
    """

    email: EmailStr


class ResetPasswordRequest(BaseModel):
    """Reset password using a valid reset token."""

    token: str
    new_password: str = Field(..., min_length=8)


# ── Email Verification ──────────────────────────────────────────────

class VerifyEmailRequest(BaseModel):
    """Verify email address with OTP code."""

    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6)
