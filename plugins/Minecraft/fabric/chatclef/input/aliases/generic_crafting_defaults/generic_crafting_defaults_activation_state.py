#20260905_kpopmodder: Name the one-way lifecycle states of a Feature-B activation.
from __future__ import annotations

from enum import Enum


class GenericCraftingDefaultsActivationState(str, Enum):
    ISSUED_UNBOUND = "ISSUED_UNBOUND"
    TRANSLATION_BOUND = "TRANSLATION_BOUND"
    SPENT = "SPENT"
    ABANDONED = "ABANDONED"
