#20260803_kpopmodder: Added status values for Korean ChatClef translation results.
from __future__ import annotations

from enum import Enum


class ChatClefIntentStatus(str, Enum):
    VALIDATED = "validated"
    UNKNOWN = "unknown"
    AMBIGUOUS = "ambiguous"
    UNSUPPORTED = "unsupported"
    INVALID = "invalid"
    INTERNAL_ERROR = "internal_error"

    @property
    def executable(self) -> bool:
        return self is ChatClefIntentStatus.VALIDATED
