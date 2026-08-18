#20260818_kpopmodder: Build the documented PreflightDecision shape without runtime side effects.
from __future__ import annotations

from typing import Mapping


def preflight_decision(
    *,
    status: str,
    selected_mutating_run: bool,
    stage: str,
    reason: str,
    observed: Mapping[str, object] | None = None,
) -> dict[str, object]:
    return {
        "status": status,
        "selected_mutating_run": selected_mutating_run,
        "stage": stage,
        "reason": reason,
        "observed": dict(observed or {}),
    }
