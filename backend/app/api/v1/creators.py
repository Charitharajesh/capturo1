"""
Capturo — Creator Routes

POST  /creators/profile                    — Create creator profile
GET   /creators/me                         — Get own creator profile
PATCH /creators/me                         — Update own creator profile
GET   /creators/nearby                     — Find nearby creators
GET   /creators                            — List all creators (public)
GET   /creators/{creator_id}               — Get public creator detail
GET   /creators/{creator_id}/availability  — Check availability
GET   /creators/{creator_id}/stats         — Creator dashboard stats
"""

from typing import List, Optional
from decimal import Decimal
from datetime import date, time

from fastapi import APIRouter, Depends, Query, Request
from jose import jwt
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.dependencies import get_current_active_user, require_role
from app.core.exceptions import ResourceNotFoundError, ForbiddenError
from app.models.user import User
from app.models.creator import CreatorProfile, CreatorFollower
from app.schemas.common import SuccessResponse, PaginatedResponse
from app.schemas.creator import (
    CreateCreatorRequest,
    UpdateCreatorRequest,
    CreatorProfileResponse,
    CreatorBriefResponse,
    CreatorPublicResponse,
    CreatorDistanceResponse,
    AvailabilityCheckResponse,
    CreatorStatsResponse,
)
from app.crud.crud_creator import crud_creator
from app.crud.crud_booking import crud_booking
from app.services.location_service import location_service

router = APIRouter()


@router.post("/profile", response_model=SuccessResponse[CreatorProfileResponse], status_code=201)
def create_profile(
    data: CreateCreatorRequest,
    current_user: User = Depends(require_role("creator")),
    db: Session = Depends(get_db),
):
    """Create a creator profile (user must have role=creator)."""
    existing = crud_creator.get_by_user_id(db, current_user.id)
    if existing:
        raise ForbiddenError("Creator profile already exists for this user")

    profile_data = data.model_dump()
    profile_data["user_id"] = current_user.id

    db_profile = CreatorProfile(**profile_data)
    db.add(db_profile)
    db.commit()
    db.refresh(db_profile)

    return SuccessResponse(
        message="Creator profile created successfully",
        data=CreatorProfileResponse.model_validate(db_profile),
    )


@router.get("/me", response_model=SuccessResponse[CreatorProfileResponse])
def get_my_profile(
    current_user: User = Depends(require_role("creator")),
    db: Session = Depends(get_db),
):
    """Get own creator profile with full stats."""
    profile = crud_creator.get_by_user_id(db, current_user.id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", current_user.id)
    
    followers_count = db.query(CreatorFollower).filter(CreatorFollower.creator_id == profile.id).count()
    resp_data = CreatorProfileResponse.model_validate(profile)
    resp_data.followers_count = followers_count
    
    return SuccessResponse(
        message="Profile retrieved",
        data=resp_data,
    )


@router.patch("/me", response_model=SuccessResponse[CreatorProfileResponse])
def update_my_profile(
    data: UpdateCreatorRequest,
    current_user: User = Depends(require_role("creator")),
    db: Session = Depends(get_db),
):
    """Update creator profile fields."""
    profile = crud_creator.get_by_user_id(db, current_user.id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", current_user.id)

    updated = crud_creator.update(db, db_obj=profile, obj_in=data)
    return SuccessResponse(
        message="Profile updated successfully",
        data=CreatorProfileResponse.model_validate(updated),
    )


@router.get("/nearby", response_model=SuccessResponse[List[CreatorDistanceResponse]])
def get_nearby(
    lat: float = Query(..., ge=-90.0, le=90.0),
    lon: float = Query(..., ge=-180.0, le=180.0),
    radius: float = Query(10.0, ge=1.0),
    specialization: Optional[str] = Query(None),
    min_rating: Optional[float] = Query(None),
    max_rate: Optional[float] = Query(None),
    db: Session = Depends(get_db),
):
    """Find creators within radius sorted by distance."""
    nearby_data = location_service.find_nearby_creators(
        db,
        user_lat=lat,
        user_lon=lon,
        radius_km=radius,
        specialization=specialization,
        min_rating=min_rating,
        max_rate=max_rate,
    )

    response_items = []
    for profile, dist in nearby_data:
        brief = CreatorBriefResponse(
            id=profile.id,
            user_id=profile.user_id,
            full_name=profile.user.full_name if profile.user else "Creator",
            email=profile.user.email if profile.user else "",
            phone=profile.user.phone if profile.user else None,
            profile_pic_url=profile.user.profile_pic_url if profile.user else None,
            hourly_rate=profile.hourly_rate,
            avg_rating=profile.avg_rating,
            total_reviews=profile.total_reviews,
            availability_status=profile.availability_status,
            latitude=profile.latitude,
            longitude=profile.longitude,
        )
        response_items.append(
            CreatorDistanceResponse(creator=brief, distance_km=Decimal(str(dist)))
        )

    return SuccessResponse(message="Nearby creators found", data=response_items)


@router.get("", response_model=PaginatedResponse[CreatorBriefResponse])
def list_creators(
    specialization: Optional[str] = Query(None),
    min_rating: Optional[float] = Query(None),
    max_rate: Optional[float] = Query(None),
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """List all creators with filters (public endpoint, no auth required)."""
    skip = (page - 1) * per_page
    items, total = crud_creator.get_creators_with_filters(
        db,
        specialization=specialization,
        min_rating=min_rating,
        max_rate=max_rate,
        skip=skip,
        limit=per_page,
    )

    creator_data = []
    for profile in items:
        creator_data.append(CreatorBriefResponse(
            id=profile.id,
            user_id=profile.user_id,
            full_name=profile.user.full_name if profile.user else "Creator",
            email=profile.user.email if profile.user else "",
            phone=profile.user.phone if profile.user else None,
            profile_pic_url=profile.user.profile_pic_url if profile.user else None,
            hourly_rate=profile.hourly_rate,
            avg_rating=profile.avg_rating,
            total_reviews=profile.total_reviews,
            availability_status=profile.availability_status,
            latitude=profile.latitude,
            longitude=profile.longitude,
        ))

    return PaginatedResponse(
        items=creator_data,
        total=total,
        page=page,
        per_page=per_page,
        has_next=(skip + per_page) < total,
        has_prev=page > 1,
    )


@router.get("/following", response_model=SuccessResponse[List[CreatorBriefResponse]])
def get_following_creators(
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Get creators followed by the current active user."""
    followers = db.query(CreatorFollower).filter(CreatorFollower.follower_id == current_user.id).all()
    creator_ids = [f.creator_id for f in followers]
    
    profiles = db.query(CreatorProfile).filter(CreatorProfile.id.in_(creator_ids)).all() if creator_ids else []
    
    creator_data = []
    for profile in profiles:
        creator_data.append(CreatorBriefResponse(
            id=profile.id,
            user_id=profile.user_id,
            full_name=profile.user.full_name if profile.user else "Creator",
            email=profile.user.email if profile.user else "",
            phone=profile.user.phone if profile.user else None,
            profile_pic_url=profile.user.profile_pic_url if profile.user else None,
            hourly_rate=profile.hourly_rate,
            avg_rating=profile.avg_rating,
            total_reviews=profile.total_reviews,
            availability_status=profile.availability_status,
            latitude=profile.latitude,
            longitude=profile.longitude,
        ))
    return SuccessResponse(message="Following creators retrieved", data=creator_data)


@router.get("/{creator_id}", response_model=SuccessResponse[CreatorPublicResponse])
def get_creator_public(
    creator_id: str,
    request: Request,
    db: Session = Depends(get_db)
):
    """Get full public creator profile by ID."""
    profile = crud_creator.get_by_user_id(db, creator_id)
    if not profile:
        profile = crud_creator.get(db, creator_id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", creator_id)

    # Count followers
    followers_count = db.query(CreatorFollower).filter(CreatorFollower.creator_id == profile.id).count()

    # Check is_following optionally
    is_following = False
    auth_header = request.headers.get("Authorization")
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.split(" ")[1]
        try:
            from app.core.config import settings
            payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
            user_id = payload.get("sub")
            if user_id:
                existing = db.query(CreatorFollower).filter(
                    CreatorFollower.follower_id == user_id,
                    CreatorFollower.creator_id == profile.id
                ).first()
                is_following = existing is not None
        except Exception:
            pass

    resp_data = CreatorPublicResponse.model_validate(profile)
    resp_data.followers_count = followers_count
    resp_data.is_following = is_following

    return SuccessResponse(
        message="Creator profile retrieved",
        data=resp_data,
    )


@router.post("/{creator_id}/follow", response_model=SuccessResponse[dict])
def follow_creator(
    creator_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Follow a creator profile."""
    profile = crud_creator.get_by_user_id(db, creator_id)
    if not profile:
        profile = crud_creator.get(db, creator_id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", creator_id)

    # Prevent self-following
    if profile.user_id == current_user.id:
        raise ForbiddenError("You cannot follow your own profile.")

    # Check if already following
    existing = db.query(CreatorFollower).filter(
        CreatorFollower.follower_id == current_user.id,
        CreatorFollower.creator_id == profile.id
    ).first()

    if not existing:
        follow = CreatorFollower(follower_id=current_user.id, creator_id=profile.id)
        db.add(follow)
        db.commit()

    followers_count = db.query(CreatorFollower).filter(CreatorFollower.creator_id == profile.id).count()

    return SuccessResponse(
        message="Creator followed successfully",
        data={
            "followers_count": followers_count,
            "is_following": True
        }
    )


@router.post("/{creator_id}/unfollow", response_model=SuccessResponse[dict])
def unfollow_creator(
    creator_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """Unfollow a creator profile."""
    profile = crud_creator.get_by_user_id(db, creator_id)
    if not profile:
        profile = crud_creator.get(db, creator_id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", creator_id)

    existing = db.query(CreatorFollower).filter(
        CreatorFollower.follower_id == current_user.id,
        CreatorFollower.creator_id == profile.id
    ).first()

    if existing:
        db.delete(existing)
        db.commit()

    followers_count = db.query(CreatorFollower).filter(CreatorFollower.creator_id == profile.id).count()

    return SuccessResponse(
        message="Creator unfollowed successfully",
        data={
            "followers_count": followers_count,
            "is_following": False
        }
    )


@router.get("/{creator_id}/availability", response_model=SuccessResponse[AvailabilityCheckResponse])
def check_availability(
    creator_id: str,
    event_date: date = Query(..., alias="date"),
    start_time: time = Query(...),
    duration: float = Query(3.0, ge=1.0),
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db),
):
    """Check if creator is available on a specific date/time."""
    is_available = crud_booking.check_availability(
        db,
        creator_id=creator_id,
        event_date=event_date,
        start_time=start_time,
        duration_hours=duration,
    )

    # Get conflicting bookings for display
    conflicting = []
    if not is_available:
        day_bookings = crud_booking.get_for_date(db, creator_id, event_date)
        conflicting = [b.id for b in day_bookings]

    return SuccessResponse(
        message="Availability checked",
        data=AvailabilityCheckResponse(
            is_available=is_available,
            conflicting_bookings=conflicting,
        ),
    )


@router.get("/{creator_id}/stats", response_model=SuccessResponse[CreatorStatsResponse])
def get_creator_stats(
    creator_id: str,
    current_user: User = Depends(require_role("creator")),
    db: Session = Depends(get_db),
):
    """Creator dashboard stats (earnings, bookings, rating). Creator-only."""
    if current_user.id != creator_id:
        raise ForbiddenError("You can only view your own stats.")

    profile = crud_creator.get_by_user_id(db, creator_id)
    if not profile:
        raise ResourceNotFoundError("CreatorProfile", creator_id)

    # Calculate monthly earnings from confirmed/paid/completed bookings
    from sqlalchemy import func
    from app.models.booking import Booking
    from datetime import datetime, timezone

    now = datetime.now(timezone.utc)
    month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    
    from datetime import timedelta
    last_month_end = month_start - timedelta(seconds=1)
    last_month_start = last_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

    # Dynamic real bookings count
    total_bookings = (
        db.query(func.count(Booking.id))
        .filter(
            Booking.creator_id == creator_id,
            Booking.status.in_(["confirmed", "paid", "completed"])
        )
        .scalar()
        or 0
    )

    monthly_earnings = (
        db.query(func.sum(Booking.total_amount))
        .filter(
            Booking.creator_id == creator_id,
            Booking.status.in_(["confirmed", "paid", "completed"]),
            Booking.event_date >= month_start.date(),
        )
        .scalar()
        or Decimal("0.00")
    )

    last_month_earnings = (
        db.query(func.sum(Booking.total_amount))
        .filter(
            Booking.creator_id == creator_id,
            Booking.status.in_(["confirmed", "paid", "completed"]),
            Booking.event_date >= last_month_start.date(),
            Booking.event_date <= last_month_end.date(),
        )
        .scalar()
        or Decimal("0.00")
    )

    if last_month_earnings > 0:
        revenue_growth_percentage = float(((monthly_earnings - last_month_earnings) / last_month_earnings) * 100)
    else:
        revenue_growth_percentage = 100.0 if monthly_earnings > 0 else 0.0

    return SuccessResponse(
        message="Creator stats retrieved",
        data=CreatorStatsResponse(
            earnings_this_month=Decimal(str(monthly_earnings)),
            bookings=total_bookings,
            rating=profile.avg_rating,
            earnings_last_month=Decimal(str(last_month_earnings)),
            revenue_growth_percentage=revenue_growth_percentage,
        ),
    )
