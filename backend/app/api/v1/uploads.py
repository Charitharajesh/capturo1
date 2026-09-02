from typing import Optional
from fastapi import APIRouter, Depends, UploadFile, File, Form
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse
from app.services.upload_service import upload_service

router = APIRouter()

@router.post("/file", response_model=SuccessResponse[dict], status_code=201)
async def upload_file(
    file: UploadFile = File(...),
    booking_id: Optional[str] = Form(None),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """General file upload endpoint returning paths"""
    file_url, thumbnail_url = await upload_service.save_uploaded_file(
        file, creator_id=current_user.id, booking_id=booking_id
    )
    return SuccessResponse(
        message="File uploaded successfully",
        data={
            "file_url": file_url,
            "thumbnail_url": thumbnail_url,
            "filename": file.filename,
            "content_type": file.content_type
        }
    )
