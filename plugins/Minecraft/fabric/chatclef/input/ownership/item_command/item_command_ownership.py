#20260905_kpopmodder: Name the sole typed ownership outcomes for request-local item commands.
from __future__ import annotations

from enum import Enum


class ItemCommandOwnership(str, Enum):
    OWNED_VALID = "OWNED_VALID"
    OWNED_INVALID = "OWNED_INVALID"
    UNRELATED = "UNRELATED"
