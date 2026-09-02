from datetime import datetime, timezone, date, time
from typing import Union

def format_datetime_iso(dt: datetime) -> str:
    """Format timezone-aware datetime into standard ISO-8601 string"""
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.isoformat().replace("+00:00", "Z")

def parse_iso_datetime(dt_str: str) -> datetime:
    """Parse ISO-8601 datetime string into timezone-aware UTC datetime"""
    dt = datetime.fromisoformat(dt_str.replace("Z", "+00:00"))
    return dt.astimezone(timezone.utc)

def combine_date_time_utc(d: date, t: time) -> datetime:
    """Combine date and time objects into UTC timezone-aware datetime"""
    return datetime.combine(d, t, tzinfo=timezone.utc)
