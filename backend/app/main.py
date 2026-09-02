"""
Capturo — FastAPI Application Entry Point

Configures the FastAPI app instance with:
- CORS middleware (Rule Set 11)
- SlowAPI rate limiting
- Request logging middleware (Rule Set 13)
- Custom exception handlers for all CapturoBaseException subclasses (Rule Set 12)
- Global 500 handler that never exposes stack traces
- Static file serving for uploads
- Lifespan events for startup/shutdown
- Health check endpoint
# Trigger reload to load updated login endpoint schemas
"""

import uvicorn
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from app.core.config import settings
from app.core.exceptions import (
    CapturoBaseException,
    ResourceNotFoundError,
    UnauthorizedError,
    ForbiddenError,
    AccountDeactivatedError,
    BookingConflictError,
    CreatorUnavailableError,
    DuplicateResourceError,
    PaymentFailedError,
    PaymentVerificationError,
    MediaUploadError,
    ReviewNotAllowedError,
    InvalidTokenError,
    RateLimitError,
)
from app.db.session import check_db_connection
from app.utils.logger import logger, setup_logging
setup_logging(debug=settings.DEBUG)
from app.core.middleware import RequestLoggingMiddleware
from app.api.v1.router import api_v1_router


# ── SlowAPI Limiter ──────────────────────────────────────────────────
limiter = Limiter(key_func=get_remote_address)


# ── Lifespan ─────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan: verify DB on startup, log on shutdown."""
    logger.info("capturo_api_startup", version=settings.VERSION)
    try:
        check_db_connection()
        logger.info("db_connection_verified")
    except Exception as e:
        logger.error("db_connection_failed", error=str(e))
    yield
    logger.info("capturo_api_shutdown")


# ── App Instance ─────────────────────────────────────────────────────
app = FastAPI(
    title="Capturo API",
    description="Photography & Videography Booking Platform — Backend Service",
    version=settings.VERSION,
    docs_url="/docs" if settings.DEBUG else None,
    redoc_url=None,
    lifespan=lifespan,
)

# ── Middleware ────────────────────────────────────────────────────────
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)

app.add_middleware(RequestLoggingMiddleware)


# ── Exception Handlers (Rule Set 12) ─────────────────────────────────
# Maps every typed CapturoBaseException subclass to its HTTP status code
# and machine-readable error_code. The loop pattern avoids closure issues
# by binding status and code as default arguments.
exception_map = {
    ResourceNotFoundError:    (404, "resource_not_found"),
    UnauthorizedError:        (401, "unauthorized"),
    InvalidTokenError:        (401, "invalid_token"),
    ForbiddenError:           (403, "forbidden"),
    AccountDeactivatedError:  (403, "account_deactivated"),
    ReviewNotAllowedError:    (403, "review_not_allowed"),
    DuplicateResourceError:   (409, "duplicate_resource"),
    BookingConflictError:     (409, "booking_conflict"),
    CreatorUnavailableError:  (409, "creator_unavailable"),
    PaymentFailedError:       (402, "payment_failed"),
    PaymentVerificationError: (400, "payment_verification_failed"),
    MediaUploadError:         (422, "media_upload_error"),
    RateLimitError:           (429, "rate_limit_exceeded"),
}

for exc_class, (status_code, error_code) in exception_map.items():

    @app.exception_handler(exc_class)
    async def _handler(request, exc, status=status_code, code=error_code):
        return JSONResponse(
            status_code=status,
            content={
                "success": False,
                "error_code": code,
                "message": exc.message,
                "details": exc.details,
            },
        )


@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Global 500 handler — NEVER expose stack traces to the client."""
    logger.error("unhandled_exception", error=str(exc), exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error_code": "internal_server_error",
            "message": "An unexpected error occurred. Please try again.",
        },
    )


# ── Static Files ─────────────────────────────────────────────────────
import os
os.makedirs("uploads", exist_ok=True)
os.makedirs("media", exist_ok=True)
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")
app.mount("/media", StaticFiles(directory="media"), name="media")

# ── API Router ───────────────────────────────────────────────────────
app.include_router(api_v1_router, prefix="/api/v1")


# ── Health Check ─────────────────────────────────────────────────────
@app.get("/health", tags=["Health"])
def health_check():
    """Lightweight health check for load balancers and monitoring."""
    return {
        "status": "ok",
        "version": settings.VERSION,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


# ── Entrypoint ───────────────────────────────────────────────────────
if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=settings.DEBUG)
