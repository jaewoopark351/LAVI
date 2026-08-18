#20260819_kpopmodder: Validate live mutation opt-ins with exact boolean semantics.
from __future__ import annotations

from collections.abc import Mapping


def live_mutating_run_selected(environment: Mapping[str, object]) -> bool:
    return (
        environment.get("live_opt_in") is True
        and environment.get("mutating_opt_in") is True
    )
