# app/services/ai/cache_service.py

import redis.asyncio as redis
import hashlib
import json
from app.core.config import settings
from app.utils.logger import get_logger

logger = get_logger("ai.cache_service")

class AICacheService:
    """
    Usage:
        cached = await ai_cache.get(key)
        if cached: return cached
        result = await call_mistral(...)
        await ai_cache.set(key, result, ttl=1800)
        return result
    """
    def __init__(self):
        self._redis = None
        try:
            self._redis = redis.from_url(settings.REDIS_URL, decode_responses=True)
        except Exception as e:
            logger.warn("redis_init_failed", error=str(e), msg="AI Caching running in fallback (no cache) mode.")

    def make_key(self, feature: str, **kwargs) -> str:
        """Create a deterministic cache key from feature name + input params."""
        content = json.dumps(kwargs, sort_keys=True)
        hash_val = hashlib.sha256(content.encode()).hexdigest()[:16]
        return f"ai:{feature}:{hash_val}"

    async def get(self, key: str) -> dict | None:
        if not self._redis:
            return None
        try:
            val = await self._redis.get(key)
            return json.loads(val) if val else None
        except Exception as e:
            logger.warn("redis_get_error", key=key, error=str(e))
            return None

    async def set(self, key: str, value: dict, ttl: int = 1800) -> None:
        if not self._redis:
            return
        try:
            await self._redis.set(key, json.dumps(value), ex=ttl)
        except Exception as e:
            logger.warn("redis_set_error", key=key, error=str(e))

    async def invalidate(self, pattern: str) -> None:
        if not self._redis:
            return
        try:
            keys = await self._redis.keys(f"ai:{pattern}:*")
            if keys:
                await self._redis.delete(*keys)
        except Exception as e:
            logger.warn("redis_invalidate_error", pattern=pattern, error=str(e))


ai_cache = AICacheService()   # ← singleton

# TTL settings (seconds):
# search results:       600   (10 min — search filters change often)
# creator matching:    1800   (30 min)
# price suggestion:    3600   (1 hour — pricing is stable)
# review summary:     86400   (24 hours — reviews don't change often)
# caption:            86400   (24 hours — same photo = same caption)
# chat suggestions:      60   (1 min — context-specific)
# earnings insights:   3600   (1 hour)
