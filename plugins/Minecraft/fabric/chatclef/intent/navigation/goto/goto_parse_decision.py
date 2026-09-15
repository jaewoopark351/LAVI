#20260913_kpopmodder: Keep the four coordinate-input routing meanings distinct.
from enum import Enum


class GotoParseDecision(str, Enum):
    NOT_CANDIDATE = "not_candidate"
    NONCOMMAND = "noncommand"
    CLARIFY = "clarify"
    VALID_XYZ = "valid_xyz"
    #20260915_kpopmodder: Additional native forms retain a distinct immutable input binding.
    VALID_VARIANT = "valid_variant"
