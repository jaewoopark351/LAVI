#20260818_kpopmodder: Build privacy-bounded summaries for supervised batch runs.
from __future__ import annotations

from collections.abc import Mapping

from ..preflight.command_fingerprint import command_fingerprint
from ..submission.invocation_fingerprint import invocation_fingerprint


def new_batch_run_result(planned_count: int) -> dict[str, object]:
    return {
        "status": "not_started",
        "stop_reason": "",
        "planned_count": planned_count,
        "attempted_count": 0,
        "completed_count": 0,
        "automatic_retry_count": 0,
        "automatic_replay_count": 0,
        "steps": [],
    }


def append_batch_step(
    batch_result: dict[str, object],
    environment: Mapping[str, object],
    command_result: Mapping[str, object] | None,
    *,
    baseline_status: str,
    checkpoint_status: str,
    guard_reconciliation_status: str,
) -> None:
    payload = dict(command_result or {})
    preflight = _mapping(payload.get("preflight"))
    observation = _mapping(payload.get("observation"))
    steps = batch_result.get("steps")
    if not isinstance(steps, list):
        raise TypeError("batch result steps must be a list")
    steps.append(
        {
            "command_fingerprint": command_fingerprint(environment.get("command")),
            "invocation_fingerprint": invocation_fingerprint(
                environment.get("invocation_id")
            ),
            "preflight_status": preflight.get("status", "absent"),
            "submission_outcome": observation.get(
                "submission_outcome",
                "absent",
            ),
            "gradio_submit_call_count": observation.get(
                "gradio_submit_call_count",
                0,
            ),
            "terminal_status": observation.get("terminal_status", "absent"),
            "terminal_lifecycle_observed": observation.get(
                "terminal_lifecycle_observed"
            ),
            "runtime_reported_completion": observation.get(
                "runtime_reported_completion"
            ),
            "end_to_end_success": observation.get("end_to_end_success"),
            "gameplay_oracle_scope": observation.get("gameplay_oracle_scope"),
            "expected_item_delta": observation.get("expected_item_delta"),
            "observed_item_delta": observation.get("observed_item_delta"),
            "baseline_status": baseline_status,
            "checkpoint_status": checkpoint_status,
            "guard_reconciliation_status": guard_reconciliation_status,
        }
    )


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}
