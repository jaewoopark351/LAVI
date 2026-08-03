#20260803_kpopmodder: Added intent names separate from ChatClef command strings.
from __future__ import annotations

from enum import Enum


class ChatClefIntentType(str, Enum):
    GET_ITEM = "get_item"
    FOOD = "food"
    MEAT = "meat"
    GOTO = "goto"
    FOLLOW = "follow"
    IDLE = "idle"
    STOP = "stop"
    UNKNOWN = "unknown"
