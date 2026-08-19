#20260803_kpopmodder: Added intent names separate from ChatClef command strings.
from __future__ import annotations

from enum import Enum


class ChatClefIntentType(str, Enum):
    GET_ITEM = "get_item"
    EQUIP_ITEM = "equip_item"
    DEPOSIT_ITEM = "deposit_item"
    GIVE_ITEM = "give_item"
    FOOD = "food"
    MEAT = "meat"
    GOTO = "goto"
    FOLLOW = "follow"
    IDLE = "idle"
    STOP = "stop"
    UNKNOWN = "unknown"
