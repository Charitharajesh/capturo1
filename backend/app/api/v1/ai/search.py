from fastapi import APIRouter, Depends
from app.core.dependencies import get_current_active_user
from app.services.ai.search_service import search_service
from app.services.ai.rate_limiter import check_ai_rate_limit
from app.schemas.ai.search_schema import SearchParseRequest, SearchParseResponse
from app.schemas.common import SuccessResponse
from app.models.user import User

router = APIRouter()

@router.post("/search/parse", response_model=SuccessResponse[SearchParseResponse])
async def parse_search_query(
    request: SearchParseRequest,
    current_user: User = Depends(get_current_active_user),
):
    await check_ai_rate_limit(current_user.id, feature="search", limit=10)
    parsed_filters = await search_service.parse_query(request.query)
    return SuccessResponse(
        message="Search query parsed by AI",
        data=SearchParseResponse(**parsed_filters)
    )
