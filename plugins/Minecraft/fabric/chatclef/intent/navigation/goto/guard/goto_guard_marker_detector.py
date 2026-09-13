#20260913_kpopmodder: Preserve malformed as well as valid GOTO markers before optional extraction.
from collections.abc import Mapping

from .goto_guard_fields import GUARD_SLOT, GUARD_SOURCE


class GotoGuardMarkerDetector:
    def has_marker(self, intent: object) -> bool:
        if getattr(intent, "source", None) == GUARD_SOURCE:
            return True
        slots = getattr(intent, "slots", None)
        return isinstance(slots, Mapping) and GUARD_SLOT in slots
