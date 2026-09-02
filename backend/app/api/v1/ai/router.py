from fastapi import APIRouter
from app.api.v1.ai import search, matching, pricing, chat, reviews, captions, insights

router = APIRouter()

router.include_router(search.router, tags=["ai"])
router.include_router(matching.router, tags=["ai"])
router.include_router(pricing.router, tags=["ai"])
router.include_router(chat.router, tags=["ai"])
router.include_router(reviews.router, tags=["ai"])
router.include_router(captions.router, tags=["ai"])
router.include_router(insights.router, tags=["ai"])
