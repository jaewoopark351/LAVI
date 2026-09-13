#20260913_kpopmodder: Preserve original validated XYZ in a deeply immutable result.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    ChatClefNumericConstraints,
)

from .goto_parse_decision import GotoParseDecision


@dataclass(frozen=True)
class GotoParseResult:
    decision: GotoParseDecision
    xyz: tuple[int, int, int] | None = None
    reason_code: str = ""
    message: str = ""

    def __post_init__(self) -> None:
        if not isinstance(self.decision, GotoParseDecision):
            raise TypeError("goto_decision_must_be_typed")
        if not isinstance(self.reason_code, str) or not isinstance(self.message, str):
            raise TypeError("goto_rejection_metadata_must_be_text")
        if self.decision is GotoParseDecision.VALID_XYZ:
            if type(self.xyz) is not tuple or len(self.xyz) != 3:
                raise TypeError("goto_xyz_must_be_three_integer_tuple")
            for field_name, value in zip(("x", "y", "z"), self.xyz):
                ChatClefNumericConstraints.java_int(value, field_name)
        elif self.xyz is not None:
            raise ValueError("nonexecutable_goto_result_must_not_carry_xyz")

    @property
    def candidate(self) -> bool:
        return self.decision is not GotoParseDecision.NOT_CANDIDATE

    @property
    def executable(self) -> bool:
        return self.decision is GotoParseDecision.VALID_XYZ
