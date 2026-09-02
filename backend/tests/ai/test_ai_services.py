import pytest
from app.services.ai.search_service import search_service
from app.services.ai.matching_service import matching_service
from app.services.ai.pricing_service import pricing_service
from app.services.ai.chat_service import chat_service
from app.services.ai.review_service import review_service
from app.services.ai.caption_service import caption_service
from app.services.ai.insights_service import insights_service

@pytest.mark.asyncio
async def test_search_service():
    result = await search_service.parse_query("wedding photographer in Chennai under 5000")
    assert "specializations" in result
    assert "location_hint" in result
    assert result["location_hint"] == "Chennai"
    assert "wedding" in result["specializations"]

@pytest.mark.asyncio
async def test_matching_service():
    booking_context = {
        "event_type": "wedding",
        "location": "Chennai",
        "budget_inr": 5000,
        "event_date": "2026-06-15"
    }
    creators = [
        {"id": "c1", "specializations": ["wedding"], "hourly_rate": 3000, "avg_rating": 4.8, "distance_km": 2.5},
        {"id": "c2", "specializations": ["wedding"], "hourly_rate": 4500, "avg_rating": 4.5, "distance_km": 5.0}
    ]
    ranked = await matching_service.rank_creators(booking_context, creators)
    assert len(ranked) == 2
    assert "ai_score" in ranked[0]
    assert "ai_reason" in ranked[0]

@pytest.mark.asyncio
async def test_pricing_service():
    result = await pricing_service.suggest_price("wedding", 4.0, "Chennai", 3000)
    assert "suggested_min" in result
    assert "suggested_max" in result
    assert "market_avg" in result
    assert "reasoning" in result

@pytest.mark.asyncio
async def test_chat_service():
    result = await chat_service.suggest_reply("Hi, what is your standard package rate?", "user", "")
    assert "suggestions" in result
    assert len(result["suggestions"]) > 0

@pytest.mark.asyncio
async def test_review_service():
    reviews = [
        {"comment": "Stunning candid shots", "rating": 5},
        {"comment": "Punctual and very polite", "rating": 5},
        {"comment": "Highly recommend for weddings", "rating": 5}
    ]
    result = await review_service.summarise_reviews("c1", "Sarah Johnson", reviews)
    assert result is not None
    assert "summary" in result
    assert "highlights" in result

@pytest.mark.asyncio
async def test_caption_service():
    result = await caption_service.generate_caption("wedding", "Chennai", "image/jpeg")
    assert "caption" in result
    assert "tags" in result

@pytest.mark.asyncio
async def test_insights_service():
    stats = {
        "total_earnings": 150000,
        "booking_count": 10,
        "completed": 8,
        "avg_booking_value": 15000,
        "top_event_type": "wedding",
        "avg_rating": 4.9,
        "monthly_breakdown": "April: 50k, May: 100k"
    }
    result = await insights_service.get_insights(stats)
    assert "trend" in result
    assert "forecast_next_month_inr" in result
    assert "recommendations" in result
