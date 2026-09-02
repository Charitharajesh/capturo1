# app/api/v1/ai/matching.py

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.dependencies import get_db, get_current_active_user
from app.services.ai.matching_service import matching_service
from app.services.ai.rate_limiter import check_ai_rate_limit
from app.schemas.ai.matching_schema import MatchRequest, MatchResponse
from app.schemas.common import SuccessResponse
from app.models.user import User

router = APIRouter()

@router.post("/creators/match", response_model=SuccessResponse[MatchResponse])
async def ai_rank_creators(
    request: MatchRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """
    AI-powered creator ranking for a specific booking request.
    """
    await check_ai_rate_limit(current_user.id, feature="matching", limit=20)

    ranked_creators = await matching_service.rank_creators(
        booking_context=request.booking_context.model_dump(),
        creators=[c.model_dump() for c in request.creators],
    )

    return SuccessResponse(
        message="Creators ranked by AI fit score",
        data=MatchResponse(creators=ranked_creators, ai_powered=True)
    )
