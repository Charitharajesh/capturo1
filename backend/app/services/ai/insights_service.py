# app/services/ai/insights_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class InsightsService:
    async def get_insights(self, stats: dict) -> dict:
        """
        Generates business insights and earnings forecast for creators.
        """
        # Create cache key based on dynamic stats values
        cache_key = ai_cache.make_key(
            "insights",
            total_earnings=stats.get("total_earnings", 0),
            booking_count=stats.get("booking_count", 0),
            completed=stats.get("completed", 0),
            avg_booking_value=stats.get("avg_booking_value", 0),
            top_event_type=stats.get("top_event_type", ""),
            avg_rating=stats.get("avg_rating", 0)
        )
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-medium-latest",
                messages=[
                    {"role": "system", "content": Prompts.INSIGHTS_SYSTEM},
                    {"role": "user",   "content": Prompts.insights_user(stats)},
                ],
                temperature=0.3,
                max_tokens=400,
                response_format={"type": "json_object"},
            )

            result = json.loads(response)
            parsed_result = {
                "trend": str(result["trend"]),
                "trend_pct": float(result["trend_pct"]),
                "forecast_next_month_inr": float(result["forecast_next_month_inr"]),
                "top_insight": str(result["top_insight"]),
                "recommendations": [str(r) for r in result.get("recommendations", [])]
            }
            await ai_cache.set(cache_key, parsed_result, ttl=3600)  # cache 1 hour
            return parsed_result

        except (AIServiceError, json.JSONDecodeError, KeyError, Exception):
            # Fallback insights generator
            earnings = stats.get("total_earnings", 0)
            forecast = earnings / 3 if earnings > 0 else 10000
            return {
                "trend": "stable",
                "trend_pct": 0.0,
                "forecast_next_month_inr": forecast,
                "top_insight": "Your bookings are performing consistently. Keep adding pictures to your gallery.",
                "recommendations": [
                    "Highlight your top event type in your profile description.",
                    "Proactively request reviews from past clients to build trust.",
                    "Optimize your hourly rates for peak wedding seasons."
                ]
            }

insights_service = InsightsService()
