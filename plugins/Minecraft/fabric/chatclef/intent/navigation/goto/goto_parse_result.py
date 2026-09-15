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
    #20260915_kpopmodder: Native XZ/Y/dimension forms do not invent missing coordinates.
    coordinates: tuple[int, ...] = ()
    dimension: str | None = None

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
        elif self.decision is GotoParseDecision.VALID_VARIANT:
            if self.xyz is not None or type(self.coordinates) is not tuple or len(self.coordinates) > 3:
                raise ValueError("invalid_goto_variant")
            if self.dimension not in {None, "overworld", "nether", "end"}:
                raise ValueError("invalid_goto_dimension")
            if not self.coordinates and self.dimension is None:
                raise ValueError("missing_goto_destination")
            for value in self.coordinates:
                ChatClefNumericConstraints.java_int(value, "coordinate")
        elif self.xyz is not None:
            raise ValueError("nonexecutable_goto_result_must_not_carry_xyz")
        if self.decision is not GotoParseDecision.VALID_VARIANT and (self.coordinates or self.dimension is not None):
            raise ValueError("unexpected_goto_variant_slots")

    @property
    def candidate(self) -> bool:
        return self.decision is not GotoParseDecision.NOT_CANDIDATE

    @property
    def executable(self) -> bool:
        return self.decision in {GotoParseDecision.VALID_XYZ, GotoParseDecision.VALID_VARIANT}

    @property
    def canonical_arguments(self) -> str:
        if not self.executable:
            raise ValueError("nonexecutable_goto")
        values = [str(v) for v in (self.xyz if self.xyz is not None else self.coordinates)]
        if self.dimension is not None:
            values.append(self.dimension)
        return " ".join(values)
