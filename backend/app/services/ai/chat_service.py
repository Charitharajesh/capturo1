# app/services/ai/chat_service.py

from app.services.ai.mistral_client import mistral_client, AIServiceError
from app.services.ai.prompt_builder import Prompts
from app.services.ai.cache_service import ai_cache
import json

class ChatService:
    async def suggest_reply(
        self,
        last_message: str,
        sender_role: str,
        conversation_context: str,
    ) -> dict:
        """
        Suggests 3 short reply options based on the chat context.
        """
        cache_key = ai_cache.make_key(
            "chat",
            last_message=last_message.strip(),
            sender_role=sender_role,
            conversation_context=conversation_context.strip()
        )
        cached = await ai_cache.get(cache_key)
        if cached:
            return cached

        try:
            response = await mistral_client.chat(
                model="mistral-small-latest",
                messages=[
                    {"role": "system", "content": Prompts.CHAT_SYSTEM},
                    {"role": "user",   "content": Prompts.chat_user(last_message, sender_role, conversation_context)},
                ],
                temperature=0.4,
                max_tokens=200,
                response_format={"type": "json_object"},
            )

            result = json.loads(response)
            parsed_result = {
                "suggestions": [str(s) for s in result.get("suggestions", [])[:3]]
            }
            await ai_cache.set(cache_key, parsed_result, ttl=60)  # cache 1 minute
            return parsed_result

        except (AIServiceError, json.JSONDecodeError, KeyError, Exception):
            # Graceful fallback: return standard prompts based on context
            return {
                "suggestions": [
                    "Sure! I'd love to chat more about this event.",
                    "What time and location are you thinking?",
                    "Let's set up a call to coordinate the details."
                ]
            }

chat_service = ChatService()
