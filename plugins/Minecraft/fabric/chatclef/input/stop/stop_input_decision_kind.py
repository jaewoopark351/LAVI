#20260905_kpopmodder: Keep Korean STOP classification outcomes explicit and closed.
from enum import Enum


class StopInputDecisionKind(str, Enum):
    VALID_EXACT_STOP = "valid_exact_stop"
    GUARDED_STOP_LIKE_REJECTION = "guarded_stop_like_rejection"
    UNRELATED = "unrelated"


__all__ = ("StopInputDecisionKind",)
