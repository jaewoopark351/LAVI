#20260831_kpopmodder: Keep bounded diagnostic parsing immutable and explicit.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositDiagnosticParseResult:
    ok: bool
    reason: str
    marker: str = ""
    fields: tuple[tuple[str, str], ...] = ()

    def as_mapping(self) -> dict[str, str]:
        return dict(self.fields)
