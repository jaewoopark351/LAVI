#20260803_kpopmodder: Added intent names separate from ChatClef command strings.
#20260827_kpopmodder: Add deterministic zero-slot STORE_HOME intent identity.
from __future__ import annotations

from enum import Enum


class ChatClefIntentType(str, Enum):
    GET_ITEM = "get_item"
    EQUIP_ITEM = "equip_item"
    DEPOSIT_ITEM = "deposit_item"
    STORE_HOME = "store_home"
    GIVE_ITEM = "give_item"
    FOOD = "food"
    MEAT = "meat"
    GOTO = "goto"
    FOLLOW = "follow"
    IDLE = "idle"
    STOP = "stop"
    UNKNOWN = "unknown"
