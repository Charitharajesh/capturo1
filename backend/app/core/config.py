"""
Capturo — Application Configuration

All settings are loaded from environment variables and .env file
using pydantic-settings. A cached singleton is exported as `settings`
for direct import throughout the application.
"""

from functools import lru_cache
from typing import List, Union

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Capturo application settings loaded from environment/.env."""

    # DATABASE
    DATABASE_URL: str = "mysql+pymysql://root:@127.0.0.1:3306/capturo"

    # On startup, create any missing tables from the SQLAlchemy models.
    # Lets the app bootstrap a fresh Supabase/Postgres database with no
    # separate migration step. Safe to leave on (create_all is idempotent).
    AUTO_CREATE_TABLES: bool = True

    # JWT
    SECRET_KEY: str = Field(default="supersecretkeyplaceholder", min_length=16)
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # APP
    APP_NAME: str = "Capturo"
    DEBUG: bool = True
    VERSION: str = "1.0.0"
    APP_HOST: str = "http://localhost:8000"
    ALLOWED_ORIGINS: Union[str, List[str]] = [
        "http://localhost",
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://10.0.2.2",
        "http://10.38.42.120",
        "http://10.38.42.120:8000",
    ]

    @field_validator("ALLOWED_ORIGINS", mode="after")
    @classmethod
    def _split_origins(cls, v):
        """Accept a comma-separated string (easy to set as one env var) or a list.
        '*' (any form) becomes ['*'] = allow all origins."""
        if isinstance(v, str):
            v = [part.strip() for part in v.split(",") if part.strip()]
        if "*" in v:
            return ["*"]
        return v

    # UPLOADS
    UPLOAD_DIR: str = "uploads"
    MAX_PHOTO_SIZE_MB: int = 25
    MAX_VIDEO_SIZE_MB: int = 500
    ALLOWED_PHOTO_TYPES: List[str] = ["image/jpeg", "image/png", "image/webp", "image/heic"]
    ALLOWED_VIDEO_TYPES: List[str] = ["video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"]

    # PAYMENTS (RAZORPAY)
    RAZORPAY_KEY_ID: str = "rzp_test_placeholder_key"
    RAZORPAY_KEY_SECRET: str = "rzp_test_placeholder_secret"
    RAZORPAY_WEBHOOK_SECRET: str = "rzp_test_placeholder_webhook_secret"

    # EMAIL (SMTP)
    SMTP_HOST: str = "smtp.gmail.com"
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM: str = ""

    # FIREBASE
    FIREBASE_CREDENTIALS_PATH: str = "firebase-credentials.json"

    # PAGINATION
    DEFAULT_PAGE_SIZE: int = 20
    MAX_PAGE_SIZE: int = 100

    # MISTRAL AI
    MISTRAL_API_KEY: str = "mock_mistral_api_key_for_testing"
    MISTRAL_MODEL_SMALL: str = "mistral-small-latest"
    MISTRAL_MODEL_MEDIUM: str = "mistral-medium-latest"
    MISTRAL_MODEL_LARGE: str = "mistral-large-latest"

    AI_MAX_TOKENS_SMALL: int = 300
    AI_MAX_TOKENS_MEDIUM: int = 500
    AI_MAX_TOKENS_LARGE: int = 800

    # REDIS
    REDIS_URL: str = "redis://localhost:6379/0"

    # AI Feature Flags
    AI_SMART_SEARCH_ENABLED: bool = True
    AI_CREATOR_MATCHING_ENABLED: bool = True
    AI_PRICE_INTELLIGENCE_ENABLED: bool = True
    AI_CHAT_SUGGESTIONS_ENABLED: bool = True
    AI_REVIEW_SUMMARISER_ENABLED: bool = True
    AI_CAPTION_GENERATOR_ENABLED: bool = True
    AI_EARNINGS_INSIGHTS_ENABLED: bool = True

    # COST CONTROL
    AI_DAILY_BUDGET_USD: float = 5.00
    AI_COST_TRACKING_ENABLED: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )

@lru_cache
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
