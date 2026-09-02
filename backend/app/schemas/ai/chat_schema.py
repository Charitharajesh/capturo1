from pydantic import BaseModel
from typing import List

class ChatSuggestRequest(BaseModel):
    last_message: str
    sender_role: str
    conversation_context: str

class ChatSuggestResponse(BaseModel):
    suggestions: List[str]
