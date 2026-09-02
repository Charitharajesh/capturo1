"""
Capturo — Gallery Routes

POST   /gallery/upload                          — Upload photo/video
GET    /gallery/creator/{creator_id}             — Public portfolio
GET    /gallery/delivery/{booking_id}            — Client delivery
PATCH  /gallery/{item_id}                        — Update metadata
DELETE /gallery/{item_id}                        — Delete item + file
POST   /gallery/delivery/{booking_id}/share      — Generate share link
GET    /gallery/delivery/{booking_id}/download    — Download zip
"""

from typing import Optional
from decimal import Decimal

from fastapi import APIRouter, Depends, Query, UploadFile, File, Form
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.gallery import (
    GalleryItemResponse,
    UpdateGalleryItemRequest,
    ClientDeliveryGalleryResponse,
    CreateShareLinkRequest,
    ShareLinkResponse,
    DownloadResponse,
)
from app.services.gallery_service import gallery_service
from app.crud.crud_gallery import crud_gallery

router = APIRouter()


@router.post("/upload", response_model=SuccessResponse[GalleryItemResponse], status_code=201)
async def upload_file(
    file: UploadFile = File(...),
    title: str = Form(...),
    description: str = Form(...),
    booking_id: Optional[str] = Form(None),
    is_portfolio: bool = Form(False),
    is_client_delivery: bool = Form(False),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Upload a photo or video file (multipart/form-data)."""
    item = await gallery_service.upload_gallery_item(
        db,
        file=file,
        creator_id=current_user.id,
        booking_id=booking_id,
        title=title,
        description=description,
        is_portfolio=is_portfolio,
        is_client_delivery=is_client_delivery,
    )
    return SuccessResponse(
        message="File uploaded successfully",
        data=GalleryItemResponse.model_validate(item),
    )


@router.get("/creator/{creator_id}", response_model=PaginatedResponse[GalleryItemResponse])
def get_creator_portfolio(
    creator_id: str,
    file_type: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """List public portfolio gallery for a creator."""
    skip = (page - 1) * per_page
    items, total = crud_gallery.get_portfolio_by_creator(
        db, creator_id=creator_id, file_type=file_type, skip=skip, limit=per_page
    )
    gallery_data = [GalleryItemResponse.model_validate(i) for i in items]

    return PaginatedResponse(
        items=gallery_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.get("/delivery/{booking_id}", response_model=SuccessResponse[ClientDeliveryGalleryResponse])
def get_delivery_gallery(
    booking_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get client delivery gallery for a booking."""
    items = crud_gallery.get_delivery_by_booking(db, booking_id=booking_id)
    gallery_data = [GalleryItemResponse.model_validate(i) for i in items]

    total_bytes = sum(i.file_size_bytes for i in items)
    total_gb = Decimal(str(total_bytes)) / Decimal(1024**3)

    resp = ClientDeliveryGalleryResponse(
        items=gallery_data,
        total=len(items),
        total_size_gb=round(total_gb, 2),
    )
    return SuccessResponse(message="Client delivery gallery retrieved", data=resp)


@router.post("/delivery/{booking_id}/share", response_model=SuccessResponse[ShareLinkResponse])
def create_share_link(
    booking_id: str,
    data: CreateShareLinkRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Generate a shareable public link for client delivery."""
    result = gallery_service.create_share_link(
        db,
        booking_id=booking_id,
        user_id=current_user.id,
        expires_in_days=data.expires_in_days,
    )
    return SuccessResponse(
        message="Share link created",
        data=ShareLinkResponse(
            share_url=result["share_url"],
            expires_at=result["expires_at"],
        ),
    )


@router.get("/delivery/{booking_id}/download", response_model=SuccessResponse[DownloadResponse])
def download_delivery(
    booking_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Download all delivery files as a zip archive (placeholder)."""
    items = crud_gallery.get_delivery_by_booking(db, booking_id=booking_id)
    total_bytes = sum(i.file_size_bytes for i in items)

    return SuccessResponse(
        message="Download ready",
        data=DownloadResponse(
            download_url=f"/api/v1/gallery/delivery/{booking_id}/archive.zip",
            size_bytes=total_bytes,
        ),
    )


@router.patch("/{item_id}", response_model=SuccessResponse[GalleryItemResponse])
def update_gallery_item(
    item_id: str,
    data: UpdateGalleryItemRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Update gallery item metadata (title, description, portfolio flag)."""
    item = gallery_service.update_gallery_item(
        db, item_id=item_id, user_id=current_user.id, data=data
    )
    return SuccessResponse(
        message="Gallery item updated",
        data=GalleryItemResponse.model_validate(item),
    )


@router.delete("/{item_id}", response_model=SuccessResponse[dict])
def delete_gallery_item(
    item_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Delete gallery item and remove file from disk."""
    gallery_service.delete_gallery_item(db, item_id=item_id, user_id=current_user.id)
    return SuccessResponse(message="Item deleted", data={})
