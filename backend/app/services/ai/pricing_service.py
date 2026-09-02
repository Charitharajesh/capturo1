# app/services/ai/pricing_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class PricingService:
    async def suggest_price(
        self,
        event_type: str,
        duration_hours: float,
        city: str,
        creator_rate: float,
    ) -> dict:
        """
        Suggest fair market pricing for a booking.
        """
        cache_key = ai_cache.make_key(
            "pricing",
            event_type=event_type,
            duration_hours=duration_hours,
            city=city.lower().strip(),
            creator_rate=creator_rate
        )
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-large-latest",
                messages=[
                    {"role": "system", "content": Prompts.PRICING_SYSTEM},
                    {"role": "user",   "content": Prompts.pricing_user(event_type, duration_hours, city, creator_rate)},
                ],
                temperature=0.3,
                max_tokens=400,
                response_format={"type": "json_object"},
            )

            result = json.loads(response)
            parsed_result = {
                "suggested_min": float(result["suggested_min"]),
                "suggested_max": float(result["suggested_max"]),
                "market_avg": float(result["market_avg"]),
                "reasoning": str(result["reasoning"]),
            }
            await ai_cache.set(cache_key, parsed_result, ttl=3600)  # cache 1 hour
            return parsed_result

        except (AIServiceError, json.JSONDecodeError, KeyError, Exception):
            # Fallback pricing estimation
            est_total = creator_rate * duration_hours
            return {
                "suggested_min": max(1000, est_total * 0.8),
                "suggested_max": est_total * 1.2,
                "market_avg": est_total,
                "reasoning": f"Based on the creator's standard rate of ₹{creator_rate}/hr.",
            }

pricing_service = PricingService()
