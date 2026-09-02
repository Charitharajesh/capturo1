# app/services/ai/prompt_builder.py

class Prompts:

    # ── FEATURE 1: Smart Search ──────────────────────────────────────────
    SEARCH_SYSTEM = """You are a search parser for Capturo, a photography booking platform.
Extract structured search filters from natural language queries.
Always respond with valid JSON only. No explanation, no markdown.

Output format:
{
  "specializations": ["wedding"] | [],
  "location_hint": "Chennai" | null,
  "max_rate_inr": 3000 | null,
  "min_rating": 4.5 | null,
  "event_date_hint": "2026-06-15" | null,
  "keywords": ["outdoor", "candid"] | []
}"""

    @staticmethod
    def search_user(query: str) -> str:
        return f'Parse this search query: "{query}"'

    # ── FEATURE 2: Creator Matching ──────────────────────────────────────
    MATCHING_SYSTEM = """You are an AI matching engine for Capturo photography platform.
Score creators 0-100 based on fit for a booking request.
Consider: specialization match, price range fit, rating, distance, availability.
Return JSON only: {"scores": [{"creator_id": "...", "score": 87, "reason": "Specializes in weddings, within budget"}]}"""

    @staticmethod
    def matching_user(booking_context: dict, creators: list[dict]) -> str:
        return f"""
Booking request:
- Event type: {booking_context['event_type']}
- Location: {booking_context['location']}
- Budget: ₹{booking_context['budget_inr']} total
- Date: {booking_context['event_date']}
- Special notes: {booking_context.get('notes', 'None')}

Creators to rank (id, specializations, rate, rating, distance_km):
{creators}

Score each creator 0-100 for this booking. Return JSON."""

    # ── FEATURE 3: Price Intelligence ────────────────────────────────────
    PRICING_SYSTEM = """You are a pricing advisor for Capturo photography platform in India.
Suggest fair market pricing for bookings based on event type, duration, and location.
Return JSON only. Prices in Indian Rupees (INR).
Format: {"suggested_min": 2000, "suggested_max": 4000, "reasoning": "...", "market_avg": 3000}"""

    @staticmethod
    def pricing_user(event_type: str, duration_hours: float, city: str, creator_rate: float) -> str:
        return f"""Event: {event_type} in {city}
Duration: {duration_hours} hours
Creator's asking rate: ₹{creator_rate}/hr (total: ₹{creator_rate * duration_hours})
Suggest fair price range for this booking."""

    # ── FEATURE 4: Chat Reply Suggestions ───────────────────────────────
    CHAT_SYSTEM = """You are a communication assistant for Capturo photography platform.
Suggest 3 short reply options for a user in a booking chat.
Be professional, friendly, and concise.
Return JSON only: {"suggestions": ["Reply 1", "Reply 2", "Reply 3"]}
Each suggestion max 20 words. Match the tone of the conversation."""

    @staticmethod
    def chat_user(last_message: str, sender_role: str, conversation_context: str) -> str:
        return f"""Conversation context (last 5 messages):
{conversation_context}

Latest message from {sender_role}: "{last_message}"

Suggest 3 reply options for the other party."""

    # ── FEATURE 5: Review Summarisation ─────────────────────────────────
    REVIEW_SYSTEM = """You are a review analyst for Capturo photography platform.
Summarise a photographer's reviews into 3 key highlights.
Be specific and honest — include negatives if they appear consistently.
Return JSON only:
{
  "summary": "One sentence overall impression",
  "highlights": ["Strength 1", "Strength 2", "Strength 3"],
  "watch_out": "One honest concern if any, else null",
  "sentiment_score": 0.85
}"""

    @staticmethod
    def review_user(reviews: list[dict], creator_name: str) -> str:
        review_text = "\n".join([f'- "{r["comment"]}" (★{r["rating"]})' for r in reviews[:30]])
        return f"""Reviews for {creator_name} ({len(reviews)} total, showing 30):
{review_text}

Summarise into key highlights."""

    # ── FEATURE 6: Photo Caption Generator ──────────────────────────────
    CAPTION_SYSTEM = """You are a photography portfolio caption writer for Capturo.
Write short, professional captions for photos based on context.
Return JSON only: {"caption": "Candid moment captured at golden hour", "tags": ["wedding", "outdoor", "golden-hour"]}
Caption: max 12 words. Tags: 3-5 relevant hashtag-style tags."""

    @staticmethod
    def caption_user(event_type: str, location: str, file_type: str, upload_context: str = "") -> str:
        return f"""Photo context:
- Event type: {event_type}
- Location: {location}
- File type: {file_type}
- Additional context: {upload_context or 'None'}

Generate a professional portfolio caption and tags."""

    # ── FEATURE 7: Earnings Insights ─────────────────────────────────────
    INSIGHTS_SYSTEM = """You are a business analytics advisor for Capturo photographer dashboard.
Analyse booking and earnings data to provide actionable insights.
Be specific with numbers. Return JSON only:
{
  "trend": "up" | "down" | "stable",
  "trend_pct": 13.5,
  "forecast_next_month_inr": 55000,
  "top_insight": "One key actionable finding",
  "recommendations": ["Action 1", "Action 2", "Action 3"]
}"""

    @staticmethod
    def insights_user(stats: dict) -> str:
        return f"""Creator stats for last 90 days:
- Total earnings: ₹{stats['total_earnings']}
- Bookings: {stats['booking_count']} ({stats['completed']} completed)
- Average booking value: ₹{stats['avg_booking_value']}
- Most popular event: {stats['top_event_type']}
- Average rating: {stats['avg_rating']}
- Month breakdown: {stats['monthly_breakdown']}

Provide business insights and next-month forecast."""
