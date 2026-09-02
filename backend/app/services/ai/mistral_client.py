from mistralai.client import Mistral
from app.core.config import settings
from app.utils.logger import get_logger
import time
import asyncio

logger = get_logger("ai.mistral_client")

class AIServiceError(Exception):
    """Raised when Mistral API fails. Route handlers catch this and return graceful fallback."""
    pass

class MistralClient:
    """
    Singleton wrapper. Import and use:
        from app.services.ai.mistral_client import mistral_client
        response = await mistral_client.chat(model="mistral-small-latest", messages=[...])
    """

    MODEL_COSTS = {
        # Cost per 1M tokens (input / output) in USD
        "mistral-small-latest":  {"input": 0.20,  "output": 0.60},
        "mistral-medium-latest": {"input": 2.70,  "output": 8.10},
        "mistral-large-latest":  {"input": 4.00,  "output": 12.00},
    }

    def __init__(self):
        # Allow mock key for testing
        api_key = settings.MISTRAL_API_KEY if settings.MISTRAL_API_KEY else "mock_key"
        self._client = Mistral(api_key=api_key)

    async def chat(
        self,
        model: str,
        messages: list[dict],
        temperature: float = 0.3,
        max_tokens: int = 500,
        response_format: dict = None,
    ) -> str:
        """
        Call Mistral chat API. Returns the content string.
        Retries 3 times with exponential backoff on transient errors.
        """
        # If the API key is the mock/test default, let's return a simulated response to prevent crashes
        if settings.MISTRAL_API_KEY == "mock_mistral_api_key_for_testing" or not settings.MISTRAL_API_KEY:
            return self._generate_mock_response(model, messages)

        for attempt in range(3):
            try:
                start = time.time()
                kwargs = {
                    "model": model,
                    "messages": messages,
                    "temperature": temperature,
                    "max_tokens": max_tokens,
                }
                if response_format:
                    kwargs["response_format"] = response_format

                # Mistral completion call
                # Note: Run in an executor if the SDK is blocking, or use async if native async client is preferred.
                # Mistral SDK's client.chat.complete is synchronous, so we run it in a thread pool to avoid blocking the event loop.
                loop = asyncio.get_event_loop()
                response = await loop.run_in_executor(
                    None,
                    lambda: self._client.chat.complete(**kwargs)
                )

                duration_ms = round((time.time() - start) * 1000)
                content = response.choices[0].message.content
                usage = response.usage

                cost = self._estimate_cost(model, usage.prompt_tokens, usage.completion_tokens)

                logger.info(
                    "mistral_call_success",
                    model=model,
                    prompt_tokens=usage.prompt_tokens,
                    completion_tokens=usage.completion_tokens,
                    estimated_cost_usd=cost,
                    duration_ms=duration_ms,
                )
                return content

            except Exception as e:
                if attempt == 2:
                    logger.error("mistral_call_failed", model=model, error=str(e), attempt=attempt+1)
                    raise AIServiceError(f"Mistral API failed after 3 attempts: {str(e)}")
                wait = 2 ** attempt
                logger.warn("mistral_call_retry", attempt=attempt+1, wait_seconds=wait, error=str(e))
                await asyncio.sleep(wait)

    def _estimate_cost(self, model: str, input_tokens: int, output_tokens: int) -> float:
        costs = self.MODEL_COSTS.get(model, {"input": 0, "output": 0})
        return round((input_tokens * costs["input"] + output_tokens * costs["output"]) / 1_000_000, 6)

    def _generate_mock_response(self, model: str, messages: list[dict]) -> str:
        """Returns mock responses for testing purposes when a real key isn't provided."""
        logger.info("mistral_call_mock", model=model, message_count=len(messages))
        user_message = messages[-1]["content"] if messages else ""
        system_prompt = messages[0]["content"] if messages and messages[0]["role"] == "system" else ""

        if "search parser" in system_prompt.lower() or "search" in user_message.lower():
            return '{"specializations": ["wedding"], "location_hint": "Chennai", "max_rate_inr": 5000, "min_rating": 4.5, "event_date_hint": "2026-06-15", "keywords": ["candid"]}'
        elif "matching engine" in system_prompt.lower() or "rank_creators" in user_message.lower() or "creators to rank" in user_message.lower():
            return '{"scores": [{"creator_id": "c1", "score": 95, "reason": "Highly rated wedding photographer in Chennai within budget"}, {"creator_id": "c2", "score": 82, "reason": "Good wedding portfolio, slightly higher rate"}]}'
        elif "pricing advisor" in system_prompt.lower() or "fair market pricing" in user_message.lower():
            return '{"suggested_min": 2500, "suggested_max": 4500, "reasoning": "Consistent with high ratings and peak wedding season demand in Chennai.", "market_avg": 3500}'
        elif "communication assistant" in system_prompt.lower() or "reply options" in user_message.lower():
            return '{"suggestions": ["Hi! Yes, I am available on that date.", "That sounds like a wonderful event! What is the venue?", "My package starts at ₹3000/hr. Let\'s schedule a brief call."]}'
        elif "review analyst" in system_prompt.lower() or "reviews for" in user_message.lower():
            return '{"summary": "Consistently praised for stunning candids and friendly demeanor.", "highlights": ["Excellent attention to candid detail", "Punctual and extremely polite", "High quality photo delivery"], "watch_out": "Weekend bookings fill up fast", "sentiment_score": 0.95}'
        elif "caption writer" in system_prompt.lower() or "photo context" in user_message.lower():
            return '{"caption": "Capturing timeless emotions under the golden hour sun.", "tags": ["wedding", "outdoor", "golden-hour", "timeless"]}'
        else:
            return '{"trend": "up", "trend_pct": 15.2, "forecast_next_month_inr": 48000, "top_insight": "Candid wedding photo requests drove 60% of your revenue.", "recommendations": ["Offer a wedding packages discount", "Promote candid portfolio", "Optimize profile keywords for Chennai"]}'

mistral_client = MistralClient()
