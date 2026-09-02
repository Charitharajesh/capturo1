import math
from decimal import Decimal
from typing import List, Tuple
from sqlalchemy.orm import Session
from app.models.creator import CreatorProfile
from app.crud.crud_creator import crud_creator

class LocationService:
    def haversine_distance(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Great-circle distance between two GPS coordinates in km"""
        R = 6371.0  # Earth's radius in km
        phi1, phi2 = math.radians(lat1), math.radians(lat2)
        dphi = math.radians(lat2 - lat1)
        dlambda = math.radians(lon2 - lon1)
        
        a = math.sin(dphi/2)**2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda/2)**2
        c = 2 * math.asin(math.sqrt(a))
        return R * c

    def get_bounding_box(self, lat: float, lon: float, radius_km: float) -> Tuple[float, float, float, float]:
        """Get minimum and maximum lat/lon range coordinates (Bounding Box)"""
        lat_delta = radius_km / 111.0
        lon_delta = radius_km / (111.0 * math.cos(math.radians(lat)))
        return lat - lat_delta, lat + lat_delta, lon - lon_delta, lon + lon_delta

    def find_nearby_creators(
        self,
        db: Session,
        user_lat: float,
        user_lon: float,
        radius_km: float = 10,
        specialization: str = None,
        min_rating: float = None,
        max_rate: float = None
    ) -> List[Tuple[CreatorProfile, float]]:
        """Pre-filter creators using bounding box (for SQL index search) then compute exact Haversine distances"""
        min_lat, max_lat, min_lon, max_lon = self.get_bounding_box(user_lat, user_lon, radius_km)
        
        # SQL pre-filtering
        candidates = crud_creator.get_multi_by_bounding_box(
            db,
            min_lat=min_lat,
            max_lat=max_lat,
            min_lon=min_lon,
            max_lon=max_lon,
            specialization=specialization,
            min_rating=min_rating,
            max_rate=max_rate
        )
        
        results = []
        for c in candidates:
            if c.latitude is not None and c.longitude is not None:
                dist = self.haversine_distance(user_lat, user_lon, float(c.latitude), float(c.longitude))
                if dist <= radius_km:
                    results.append((c, round(dist, 2)))
        
        # Sort by distance ascending
        results.sort(key=lambda x: x[1])
        return results

location_service = LocationService()
