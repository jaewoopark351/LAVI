#20260818_kpopmodder: Create one privacy-bounded supervised batch result.
from __future__ import annotations


def new_batch_run_result(planned_count: int) -> dict[str, object]:
    return {
        "status": "not_started",
        "stop_reason": "",
        "planned_count": planned_count,
        "attempted_count": 0,
        "completed_count": 0,
        "automatic_resubmit_count": 0,
        "automatic_rerun_count": 0,
        "automatic_retry_count": 0,
        "automatic_replay_count": 0,
        "steps": [],
    }
