import re
from typing import Any

def validate_phone_number(phone: str) -> bool:
    """Validate phone number format (supports standard country code prefixes)"""
    pattern = re.compile(r"^\+?[1-9]\d{1,14}$")
    return bool(pattern.match(phone))

def validate_uuid4(uuid_str: str) -> bool:
    """Validate string matches uuid4 format"""
    pattern = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", re.I)
    return bool(pattern.match(uuid_str))
