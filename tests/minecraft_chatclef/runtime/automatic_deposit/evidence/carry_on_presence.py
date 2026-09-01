#20260831_kpopmodder: Represent Carry On availability without guessing success.
from __future__ import annotations

from enum import Enum


class AutomaticDepositCarryOnPresence(str, Enum):
    INSTALLED = "INSTALLED"
    ABSENT = "ABSENT"
    INCOMPATIBLE = "INCOMPATIBLE"
    UNREADABLE = "UNREADABLE"
