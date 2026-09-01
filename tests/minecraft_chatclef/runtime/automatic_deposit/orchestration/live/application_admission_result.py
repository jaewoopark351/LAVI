#20260901_kpopmodder: Carry one immutable live application admission decision.
from __future__ import annotations

from dataclasses import dataclass

from ...scenario.matrix_row import AutomaticDepositMatrixRow


@dataclass(frozen=True, slots=True)
class AutomaticDepositLiveApplicationAdmissionResult:
    ok: bool
    row: AutomaticDepositMatrixRow | None
    result: dict[str, object]
