# app/services/ai/review_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class ReviewService:

    async def summarise_reviews(
        self,
        creator_id: str,
        creator_name: str,
        reviews: list[dict],   # list of {comment: str, rating: int}
    ) -> dict:
        """
        Returns:
        {
            "summary": "Highly rated wedding photographer with fast delivery",
            "highlights": ["Punctual and professional", "Beautiful candid shots", "Quick 24hr delivery"],
            "watch_out": "Some mention higher prices for weekends",
            "sentiment_score": 0.92
        }
        Falls back to None if AI fails — frontend shows plain reviews.
        """
        if len(reviews) < 3:
            return None   # not enough reviews to summarise

        cache_key = ai_cache.make_key("reviews", creator_id=creator_id, review_count=len(reviews))
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-medium-latest",
                messages=[
                    {"role": "system", "content": Prompts.REVIEW_SYSTEM},
                    {"role": "user",   "content": Prompts.review_user(reviews, creator_name)},
                ],
                temperature=0.3,
                max_tokens=300,
                response_format={"type": "json_object"},
            )
            result = json.loads(response)
            await ai_cache.set(cache_key, result, ttl=86400)   # cache 24 hours
            return result
        except (AIServiceError, json.JSONDecodeError, Exception):
            return None   # graceful fallback


review_service = ReviewService()
