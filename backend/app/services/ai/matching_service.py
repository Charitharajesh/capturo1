# app/services/ai/matching_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class MatchingService:

    async def rank_creators(
        self,
        booking_context: dict,
        creators: list[dict],
    ) -> list[dict]:
        """
        Input creators: list of dicts with id, specializations, hourly_rate, avg_rating, distance_km
        Returns: same list sorted by AI fit score, with 'ai_score' and 'ai_reason' added to each
        """
        if len(creators) <= 1:
            return creators   # no point ranking a single result

        # Check cache first
        cache_key = ai_cache.make_key("matching",
            event_type=booking_context["event_type"],
            creator_ids=sorted([c["id"] for c in creators]),
            budget=booking_context["budget_inr"]
        )
        cached = await ai_cache.get(cache_key)
        if cached:
            return self._apply_scores(creators, cached["scores"])

        try:
            # Build compact creator list for prompt (avoid sending full DB objects)
            compact_creators = [
                {
                    "id": c["id"],
                    "specializations": c["specializations"],
                    "rate_inr_per_hr": float(c["hourly_rate"]),
                    "rating": float(c["avg_rating"]),
                    "distance_km": c["distance_km"],
                }
                for c in creators
            ]

            response = await mistral_client.chat(
                model="mistral-medium-latest",
                messages=[
                    {"role": "system", "content": Prompts.MATCHING_SYSTEM},
                    {"role": "user",   "content": Prompts.matching_user(booking_context, compact_creators)},
                ],
                temperature=0.2,   # low — we want consistent scoring
                max_tokens=600,
                response_format={"type": "json_object"},
            )

            scores_data = json.loads(response)
            await ai_cache.set(cache_key, scores_data, ttl=1800)
            return self._apply_scores(creators, scores_data["scores"])

        except (AIServiceError, json.JSONDecodeError, KeyError, Exception):
            # Graceful fallback — return original distance-sorted list
            return creators

    def _apply_scores(self, creators: list[dict], scores: list[dict]) -> list[dict]:
        """Merge AI scores into creator objects and sort by score DESC."""
        score_map = {s["creator_id"]: s for s in scores}
        for c in creators:
            score_info = score_map.get(c["id"], {})
            c["ai_score"]  = score_info.get("score", 50)
            c["ai_reason"] = score_info.get("reason", "")
        return sorted(creators, key=lambda x: x.get("ai_score", 50), reverse=True)


matching_service = MatchingService()
