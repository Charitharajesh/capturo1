from typing import List, TypeVar, Generic
from pydantic import BaseModel
from app.schemas.common import PaginatedResponse

T = TypeVar("T")

def build_paginated_response(
    items: List[T],
    total: int,
    page: int,
    per_page: int
) -> PaginatedResponse[T]:
    """Helper to assemble PaginatedResponse wrapper models"""
    return PaginatedResponse(
        items=items,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(page * per_page) < total,
        has_prev=page > 1
    )
