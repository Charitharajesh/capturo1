from typing import Optional, List
from sqlalchemy.orm import Session
from app.crud.base import CRUDBase
from app.models.payment import Payment
from app.schemas.payment import CreatePaymentOrderRequest, RefundRequest

class CRUDPayment(CRUDBase[Payment, CreatePaymentOrderRequest, RefundRequest]):
    def get_by_booking_id(self, db: Session, booking_id: str) -> Optional[Payment]:
        return db.query(self.model).filter(self.model.booking_id == booking_id).first()

    def get_by_gateway_order_id(self, db: Session, gateway_order_id: str) -> Optional[Payment]:
        return db.query(self.model).filter(self.model.gateway_order_id == gateway_order_id).first()

    def get_by_gateway_payment_id(self, db: Session, gateway_payment_id: str) -> Optional[Payment]:
        return db.query(self.model).filter(self.model.gateway_payment_id == gateway_payment_id).first()

    def get_history_by_user(self, db: Session, user_id: str, skip: int = 0, limit: int = 20) -> tuple[List[Payment], int]:
        query = db.query(self.model).filter(self.model.payer_id == user_id)
        total = query.count()
        items = query.order_by(self.model.created_at.desc()).offset(skip).limit(limit).all()
        return items, total

crud_payment = CRUDPayment(Payment)
