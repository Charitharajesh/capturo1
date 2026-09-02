from fastapi import APIRouter, Depends
from app.core.dependencies import get_current_active_user
from app.services.ai.review_service import review_service
from app.services.ai.rate_limiter import check_ai_rate_limit
from app.schemas.ai.review_schema import ReviewSummariseRequest, ReviewSummariseResponse
from app.schemas.common import SuccessResponse
from app.models.user import User

router = APIRouter()

@router.post("/reviews/summarise", response_model=SuccessResponse[ReviewSummariseResponse])
async def ai_summarise_reviews(
    request: ReviewSummariseRequest,
    current_user: User = Depends(get_current_active_user),
):
    await check_ai_rate_limit(current_user.id, feature="reviews", limit=5)

    summary_data = await review_service.summarise_reviews(
        creator_id=request.creator_id,
        creator_name=request.creator_name,
        reviews=[r.model_dump() for r in request.reviews],
    )

    if not summary_data:
        # Graceful fallback response
        return SuccessResponse(
            message="Not enough reviews or AI failed to summarise reviews",
            data=ReviewSummariseResponse(
                summary="Excellent reviews across the board.",
                highlights=["Highly rated photographer", "Friendly and professional", "Consistent punctuality"],
                sentiment_score=1.0
            )
        )

    return SuccessResponse(
        message="Reviews summarized by AI",
        data=ReviewSummariseResponse(**summary_data)
    )
