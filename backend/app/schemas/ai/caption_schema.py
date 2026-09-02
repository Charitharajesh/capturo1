from pydantic import BaseModel
from typing import Optional, List

class CaptionRequest(BaseModel):
    event_type: str
    location: str
    file_type: str
    upload_context: Optional[str] = None

class CaptionResponse(BaseModel):
    caption: str
    tags: List[str]
