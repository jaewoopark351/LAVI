#20260913_kpopmodder: Define immutable no-reinterpretation metadata, never execution authority.
from types import MappingProxyType

from ..goto_parse_decision import GotoParseDecision


GUARD_SOURCE = "rule_goto_guard"
GUARD_SLOT = "goto_guarded"
DECISION_SLOT = "goto_decision"
REASON_SLOT = "reason_code"
EXPECTED_SLOTS = frozenset((GUARD_SLOT, DECISION_SLOT, REASON_SLOT))
GUARDED_REASONS = MappingProxyType({
    "goto_noncommand": GotoParseDecision.NONCOMMAND,
    "goto_control_character": GotoParseDecision.CLARIFY,
    "goto_unsupported_whitespace": GotoParseDecision.CLARIFY,
    "goto_coordinate_out_of_range": GotoParseDecision.CLARIFY,
    "goto_invalid_coordinates": GotoParseDecision.CLARIFY,
})
