# app/services/ai/rate_limiter.py

import redis.asyncio as redis
from app.core.config import settings
from app.core.exceptions import RateLimitError
from app.utils.logger import get_logger

logger = get_logger("ai.rate_limiter")

# Limits per feature per user per minute:
FEATURE_LIMITS = {
    "search":    10,   # 10 smart searches per minute
    "matching":  20,   # 20 matching requests per minute
    "pricing":   10,
    "chat":      30,   # higher — chat is real-time
    "reviews":    5,
    "captions":  10,
    "insights":   5,
}

async def check_ai_rate_limit(user_id: str, feature: str, limit: int = None) -> None:
    """
    Raises RateLimitError if user has exceeded the limit.
    Uses Redis sliding window counter with 60-second TTL.
    Gracefully degrades if Redis is offline or not configured.
    """
    try:
        r = redis.from_url(settings.REDIS_URL, decode_responses=True)
        effective_limit = limit or FEATURE_LIMITS.get(feature, 10)
        key = f"ai_rl:{feature}:{user_id}"

        current = await r.incr(key)
        if current == 1:
            await r.expire(key, 60)   # start 60-second window on first call

        if current > effective_limit:
            retry_after = await r.ttl(key)
            raise RateLimitError(
                f"AI rate limit exceeded for {feature}. "
                f"Try again in {retry_after} seconds."
            )
    except RateLimitError:
        # Re-raise the actual rate limit exception
        raise
    except Exception as e:
        # Graceful degradation: Log warning and proceed
        logger.warn("rate_limiter_connection_failed", error=str(e), msg="Skipping rate limit check because Redis is down.")
        return
