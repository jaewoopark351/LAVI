#20260913_kpopmodder: Freeze one GOTO owner's terminal meaning with its original binding.
from __future__ import annotations

from dataclasses import dataclass

from .goto_command_binding import GotoCommandBinding


@dataclass(frozen=True, slots=True)
class GotoTerminalPayload:
    binding: GotoCommandBinding
    outcome: str
    failure_reason: str
    goal_satisfied: bool
    binding_valid: bool
    children_quiescent: bool
    evidence_kind: str
    terminal_dimension: str | None


__all__ = ("GotoTerminalPayload",)
