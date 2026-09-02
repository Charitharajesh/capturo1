"""
Capturo — Custom Exception Hierarchy

All application-specific exceptions inherit from CapturoBaseException.
Each exception maps to a specific HTTP status code via the exception_map
registered in main.py. Routes and services MUST raise these typed
exceptions instead of raw HTTPException (Rule Set 12).

Exception → HTTP mapping:
    ResourceNotFoundError       → 404
    UnauthorizedError           → 401
    InvalidTokenError           → 401
    ForbiddenError              → 403
    AccountDeactivatedError     → 403
    ReviewNotAllowedError       → 403
    DuplicateResourceError      → 409
    BookingConflictError        → 409
    CreatorUnavailableError     → 409
    PaymentFailedError          → 402
    PaymentVerificationError    → 400
    MediaUploadError            → 422
"""

from typing import Any, Dict, Optional


class CapturoBaseException(Exception):
    """Base exception for all Capturo application errors.

    Attributes:
        message: Human-readable error message shown to the client.
        details: Optional dictionary with additional structured context.
    """

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None) -> None:
        self.message = message
        self.details = details
        super().__init__(message)


# ── 404 ──────────────────────────────────────────────────────────────

class ResourceNotFoundError(CapturoBaseException):
    """404 — Requested resource does not exist.

    Usage:
        raise ResourceNotFoundError("Booking", booking_id)
    """

    def __init__(self, resource: str, id: str) -> None:
        super().__init__(f"{resource} with id '{id}' not found")


# ── 401 ──────────────────────────────────────────────────────────────

class UnauthorizedError(CapturoBaseException):
    """401 — Not authenticated or invalid credentials.

    NEVER distinguish between 'email not found' and 'wrong password' —
    always use a generic message to prevent user enumeration.
    """

    def __init__(self, message: str = "Not authenticated", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


class InvalidTokenError(CapturoBaseException):
    """401 — JWT token is invalid, expired, or blacklisted."""

    def __init__(self, message: str = "Token is invalid or expired", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 403 ──────────────────────────────────────────────────────────────

class ForbiddenError(CapturoBaseException):
    """403 — Authenticated but insufficient permissions for this action."""

    def __init__(self, message: str = "Permission denied", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


class AccountDeactivatedError(CapturoBaseException):
    """403 — User account has been deactivated."""

    def __init__(self, message: str = "Account is deactivated", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


class ReviewNotAllowedError(CapturoBaseException):
    """403 — Review submission denied (booking not completed, or already reviewed)."""

    def __init__(self, message: str = "Reviews are only allowed for completed bookings", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 409 ──────────────────────────────────────────────────────────────

class DuplicateResourceError(CapturoBaseException):
    """409 — Resource already exists (e.g., email already registered).

    Usage:
        raise DuplicateResourceError("email", "user@example.com")
    """

    def __init__(self, field: str, value: str, details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(f"A resource with {field} '{value}' already exists", details)


class BookingConflictError(CapturoBaseException):
    """409 — Booking slot conflict or invalid state transition."""

    def __init__(self, message: str = "Booking slot conflict detected", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


class CreatorUnavailableError(CapturoBaseException):
    """409 — Creator is not available at the requested date/time."""

    def __init__(self, message: str = "Creator is not available at the requested time", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 402 ──────────────────────────────────────────────────────────────

class PaymentFailedError(CapturoBaseException):
    """402 — Payment processing or gateway error."""

    def __init__(self, message: str = "Payment processing failed", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 400 ──────────────────────────────────────────────────────────────

class PaymentVerificationError(CapturoBaseException):
    """400 — Razorpay signature verification failed."""

    def __init__(self, message: str = "Payment signature verification failed", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 422 ──────────────────────────────────────────────────────────────

class MediaUploadError(CapturoBaseException):
    """422 — File type, size, or content validation failed."""

    def __init__(self, message: str = "Invalid file upload content, size, or type", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)


# ── 429 ──────────────────────────────────────────────────────────────

class RateLimitError(CapturoBaseException):
    """429 — Rate limit exceeded for user requests."""

    def __init__(self, message: str = "Rate limit exceeded", details: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, details)
