import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.core.config import settings
from app.utils.logger import logger

class EmailService:
    def send_email(self, to_email: str, subject: str, body: str) -> None:
        """Send email via SMTP (Gmail or custom server config)"""
        # Validate settings first
        if not settings.SMTP_HOST or not settings.SMTP_USER or not settings.SMTP_PASSWORD:
            logger.warning("smtp_service_unconfigured", message="Skipping email delivery. Host, User, or Password unconfigured.")
            return

        try:
            msg = MIMEMultipart()
            msg["From"] = settings.SMTP_USER
            msg["To"] = to_email
            msg["Subject"] = subject
            msg.attach(MIMEText(body, "plain"))

            # Initialize server
            server = smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT)
            server.starttls()
            server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            server.send_message(msg)
            server.quit()
            logger.info("email_delivered", recipient=to_email, subject=subject)
        except Exception as e:
            logger.error("email_delivery_failed", error=str(e), recipient=to_email)

email_service = EmailService()
