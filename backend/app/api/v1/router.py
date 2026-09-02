from fastapi import APIRouter
from app.api.v1 import (
    auth,
    users,
    creators,
    bookings,
    payments,
    messages,
    reviews,
    gallery,
    notifications,
    uploads,
    admin,
    ai
)

api_v1_router = APIRouter()

api_v1_router.include_router(auth.router, prefix="/auth", tags=["auth"])
api_v1_router.include_router(users.router, prefix="/users", tags=["users"])
api_v1_router.include_router(creators.router, prefix="/creators", tags=["creators"])
api_v1_router.include_router(bookings.router, prefix="/bookings", tags=["bookings"])
api_v1_router.include_router(payments.router, prefix="/payments", tags=["payments"])
api_v1_router.include_router(messages.router, prefix="/messages", tags=["messages"])
api_v1_router.include_router(reviews.router, prefix="/reviews", tags=["reviews"])
api_v1_router.include_router(gallery.router, prefix="/gallery", tags=["gallery"])
api_v1_router.include_router(notifications.router, prefix="/notifications", tags=["notifications"])
api_v1_router.include_router(uploads.router, prefix="/uploads", tags=["uploads"])
api_v1_router.include_router(admin.router, prefix="/admin", tags=["admin"])
api_v1_router.include_router(ai.router, prefix="/ai", tags=["ai"])
