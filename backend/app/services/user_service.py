"""
Capturo — User Service

Business logic for user profile operations:
- Public profile retrieval
- Account deactivation (soft delete)
- FCM token management
"""

from sqlalchemy.orm import Session

from app.core.exceptions import ResourceNotFoundError
from app.crud.crud_user import crud_user
from app.models.user import User
from app.utils.logger import logger


class UserService:
    """User profile and account management business logic."""

    def get_public_profile(self, db: Session, user_id: str) -> User:
        """Get a user's public profile by ID.

        Returns:
            User ORM instance (route layer selects which fields to expose).

        Raises:
            ResourceNotFoundError: If user does not exist.
        """
        user = crud_user.get(db, user_id)
        if not user:
            raise ResourceNotFoundError("User", user_id)
        return user

    def deactivate_account(self, db: Session, user_id: str) -> User:
        """Soft-delete a user account by setting is_active=False.

        Returns:
            The deactivated User ORM instance.

        Raises:
            ResourceNotFoundError: If user does not exist.
        """
        user = crud_user.deactivate(db, user_id)
        if not user:
            raise ResourceNotFoundError("User", user_id)
        logger.info("account_deactivated", user_id=user_id)
        return user

    def update_fcm_token(self, db: Session, user: User, fcm_token: str) -> None:
        """Update the user's FCM device token for push notifications.

        Args:
            user: The authenticated User ORM instance.
            fcm_token: Firebase Cloud Messaging device token.
        """
        user.fcm_token = fcm_token
        db.add(user)
        db.commit()
        logger.info("fcm_token_updated", user_id=user.id)


user_service = UserService()
