from typing import Optional, List
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.crud.base import CRUDBase
from app.models.creator import CreatorProfile
from app.schemas.creator import CreateCreatorRequest, UpdateCreatorRequest

class CRUDCreator(CRUDBase[CreatorProfile, CreateCreatorRequest, UpdateCreatorRequest]):
    def get_by_user_id(self, db: Session, user_id: str) -> Optional[CreatorProfile]:
        return db.query(self.model).filter(self.model.user_id == user_id).first()

    def get_multi_by_bounding_box(
        self,
        db: Session,
        min_lat: float,
        max_lat: float,
        min_lon: float,
        max_lon: float,
        specialization: Optional[str] = None,
        min_rating: Optional[float] = None,
        max_rate: Optional[float] = None
    ) -> List[CreatorProfile]:
        query = db.query(self.model).filter(
            self.model.latitude.between(min_lat, max_lat),
            self.model.longitude.between(min_lon, max_lon),
            self.model.availability_status == "available"
        )
        if specialization:
            query = query.filter(func.json_contains(self.model.specializations, func.json_quote(specialization)))
        if min_rating:
            query = query.filter(self.model.avg_rating >= min_rating)
        if max_rate:
            query = query.filter(self.model.hourly_rate <= max_rate)
        return query.all()

    def get_creators_with_filters(
        self,
        db: Session,
        specialization: Optional[str] = None,
        min_rating: Optional[float] = None,
        max_rate: Optional[float] = None,
        skip: int = 0,
        limit: int = 20
    ) -> tuple[List[CreatorProfile], int]:
        query = db.query(self.model)
        if specialization:
            query = query.filter(func.json_contains(self.model.specializations, func.json_quote(specialization)))
        if min_rating:
            query = query.filter(self.model.avg_rating >= min_rating)
        if max_rate:
            query = query.filter(self.model.hourly_rate <= max_rate)
        
        total = query.count()
        items = query.offset(skip).limit(limit).all()
        return items, total

crud_creator = CRUDCreator(CreatorProfile)
