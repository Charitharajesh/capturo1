# app/services/ai/caption_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class CaptionService:
    async def generate_caption(
        self,
        event_type: str,
        location: str,
        file_type: str,
        upload_context: str = "",
    ) -> dict:
        """
        Generates portfolio captions and hashtags for uploaded photos.
        """
        cache_key = ai_cache.make_key(
            "caption",
            event_type=event_type,
            location=location.lower().strip(),
            file_type=file_type,
            upload_context=upload_context.strip()
        )
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-small-latest",
                messages=[
                    {"role": "system", "content": Prompts.CAPTION_SYSTEM},
                    {"role": "user",   "content": Prompts.caption_user(event_type, location, file_type, upload_context)},
                ],
                temperature=0.6,
                max_tokens=150,
                response_format={"type": "json_object"},
            )

            result = json.loads(response)
            parsed_result = {
                "caption": str(result["caption"]),
                "tags": [str(t) for t in result.get("tags", [])]
            }
            await ai_cache.set(cache_key, parsed_result, ttl=86400)  # cache 24 hours
            return parsed_result

        except (AIServiceError, json.JSONDecodeError, KeyError, Exception):
            # Fallback
            return {
                "caption": f"Capturing a wonderful {event_type} memory in {location}.",
                "tags": [event_type.lower(), "photography", location.lower().replace(" ", "-")]
            }

caption_service = CaptionService()
