from datetime import datetime, timezone
from typing import Generic, TypeVar, Optional, List, Annotated, Any
from pydantic import BaseModel, ConfigDict, BeforeValidator
from fastapi import Query

def make_tz_aware(v: Any) -> Any:
    if isinstance(v, datetime) and v.tzinfo is None:
        return v.replace(tzinfo=timezone.utc)
    return v

DateTimeUTC = Annotated[datetime, BeforeValidator(make_tz_aware)]

T = TypeVar("T")

class SuccessResponse(BaseModel, Generic[T]):
    success: bool = True
    message: str
    data: Optional[T] = None
    model_config = ConfigDict(from_attributes=True)

class PaginatedResponse(BaseModel, Generic[T]):
    success: bool = True
    items: List[T]
    total: int
    page: int
    per_page: int
    has_next: bool
    has_prev: bool
    model_config = ConfigDict(from_attributes=True)

class ErrorResponse(BaseModel):
    success: bool = False
    error_code: str
    message: str
    details: Optional[dict] = None

class PaginationParams:
    def __init__(self, page: int = Query(1, ge=1), per_page: int = Query(20, ge=1, le=100)):
        self.page = page
        self.per_page = per_page
        self.skip = (page - 1) * per_page
        self.limit = per_page
