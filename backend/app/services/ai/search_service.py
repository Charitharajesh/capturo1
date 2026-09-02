# app/services/ai/search_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class SearchService:
    async def parse_query(self, query: str) -> dict:
        """
        Parses a natural language search query into structured search filters.
        Returns:
            dict containing:
                specializations: list
                location_hint: str | None
                max_rate_inr: float | None
                min_rating: float | None
                event_date_hint: str | None
                keywords: list
        """
        if not query or len(query.strip()) < 3:
            return self._empty_fallback()

        cache_key = ai_cache.make_key("search", query=query.strip().lower())
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-small-latest",
                messages=[
                    {"role": "system", "content": Prompts.SEARCH_SYSTEM},
                    {"role": "user",   "content": Prompts.search_user(query)},
                ],
                temperature=0.1,
                max_tokens=300,
                response_format={"type": "json_object"},
            )

            result = json.loads(response)
            # Ensure correct format structure
            parsed_result = {
                "specializations": result.get("specializations", []),
                "location_hint": result.get("location_hint", None),
                "max_rate_inr": result.get("max_rate_inr", None),
                "min_rating": result.get("min_rating", None),
                "event_date_hint": result.get("event_date_hint", None),
                "keywords": result.get("keywords", []),
            }
            await ai_cache.set(cache_key, parsed_result, ttl=600)  # cache 10 mins
            return parsed_result

        except (AIServiceError, json.JSONDecodeError, Exception):
            return self._empty_fallback()

    def _empty_fallback(self) -> dict:
        return {
            "specializations": [],
            "location_hint": None,
            "max_rate_inr": None,
            "min_rating": None,
            "event_date_hint": None,
            "keywords": [],
        }

search_service = SearchService()
