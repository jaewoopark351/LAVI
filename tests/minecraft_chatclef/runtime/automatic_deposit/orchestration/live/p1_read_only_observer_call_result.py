#20260901_kpopmodder: Carry one bounded P1 observer-call outcome without raw exception text.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class P1ReadOnlyObserverCallResult:
    ok: bool
    observer_name: str
    reason: str
    source_reason: str = ""
    error_type: str = ""
    value: object | None = field(default=None, repr=False)
