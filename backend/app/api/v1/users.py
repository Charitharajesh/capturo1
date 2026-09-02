"""
Capturo — User Routes

GET    /users/me           — Get own profile
PATCH  /users/me           — Update own profile
POST   /users/me/fcm-token — Register FCM device token
GET    /users/{user_id}    — Get public profile by ID
DELETE /users/me           — Soft-delete own account
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user
from app.models.user import User
from app.schemas.common import SuccessResponse
from app.schemas.user import UserResponse, UserPublicResponse, UpdateUserRequest, FCMTokenRequest
from app.crud.crud_user import crud_user
from app.services.user_service import user_service

router = APIRouter()


@router.get("/me", response_model=SuccessResponse[UserResponse])
def get_me(current_user: User = Depends(get_current_active_user)):
    """Get the authenticated user's full profile."""
    return SuccessResponse(
        message="Profile retrieved",
        data=UserResponse.model_validate(current_user),
    )


@router.patch("/me", response_model=SuccessResponse[UserResponse])
def update_me(
    data: UpdateUserRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Update the authenticated user's profile fields."""
    updated_user = crud_user.update(db, db_obj=current_user, obj_in=data)
    return SuccessResponse(
        message="Profile updated successfully",
        data=UserResponse.model_validate(updated_user),
    )


@router.post("/me/fcm-token", response_model=SuccessResponse[dict])
def update_fcm_token(
    data: FCMTokenRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Register or update device FCM token for push notifications."""
    user_service.update_fcm_token(db, current_user, data.fcm_token)
    return SuccessResponse(
        message="FCM Token updated successfully",
        data={"fcm_token": data.fcm_token},
    )


@router.delete("/me", response_model=SuccessResponse[dict])
def deactivate_me(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Soft-delete own account (sets is_active=False)."""
    user_service.deactivate_account(db, current_user.id)
    return SuccessResponse(message="Account deactivated", data={})


@router.get("/{user_id}", response_model=SuccessResponse[UserPublicResponse])
def get_user_public(
    user_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Get public profile of any user by ID."""
    user = user_service.get_public_profile(db, user_id)
    return SuccessResponse(
        message="User profile retrieved",
        data=UserPublicResponse.model_validate(user),
    )
