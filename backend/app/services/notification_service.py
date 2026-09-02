import os
from typing import Optional
from sqlalchemy.orm import Session
import firebase_admin
from firebase_admin import credentials, messaging
from app.core.config import settings
from app.crud.crud_notification import crud_notification, CreateNotificationInternal
from app.crud.crud_user import crud_user
from app.utils.logger import logger

class NotificationService:
    def __init__(self):
        self.firebase_initialized = False
        try:
            # Try to initialize Firebase Admin SDK if credentials exist
            cred_path = settings.FIREBASE_CREDENTIALS_PATH
            if cred_path and os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                self.firebase_initialized = True
        except Exception as e:
            logger.warning("firebase_init_skipped", detail=str(e))

    def send_notification(
        self,
        db: Session,
        user_id: str,
        title: str,
        body: str,
        notification_type: str,
        reference_id: Optional[str] = None,
        reference_type: Optional[str] = None
    ) -> None:
        """Create database notification record and dispatch to Firebase Cloud Messaging (FCM)"""
        # Save to DB
        notification_in = CreateNotificationInternal(
            user_id=user_id,
            title=title,
            body=body,
            notification_type=notification_type,
            reference_id=reference_id,
            reference_type=reference_type
        )
        crud_notification.create(db, obj_in=notification_in)

        # Retrieve user device token
        user = crud_user.get(db, user_id)
        if user and user.fcm_token:
            self._send_fcm(user.fcm_token, title, body, {
                "notification_type": notification_type,
                "reference_id": reference_id or "",
                "reference_type": reference_type or ""
            })

    def _send_fcm(self, token: str, title: str, body: str, data: dict) -> None:
        if not self.firebase_initialized:
            logger.info("fcm_simulated", token=token, title=title, body=body)
            return

        try:
            message = messaging.Message(
                notification=messaging.Notification(title=title, body=body),
                data=data,
                token=token
            )
            messaging.send(message)
            logger.info("fcm_sent", token=token, title=title)
        except Exception as e:
            logger.error("fcm_failed", error=str(e), token=token)

notification_service = NotificationService()
