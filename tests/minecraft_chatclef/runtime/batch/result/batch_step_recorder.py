#20260818_kpopmodder: Record and aggregate one supervised batch step.
from __future__ import annotations

from collections.abc import Mapping

from ...preflight.command_fingerprint import command_fingerprint
from ...submission.invocation_fingerprint import invocation_fingerprint
from .batch_automatic_count_aggregator import aggregate_automatic_counts


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
    resubmit_count = observation.get("automatic_resubmit_count", "absent")
    rerun_count = observation.get("automatic_rerun_count", "absent")
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
            "automatic_resubmit_count": resubmit_count,
            "automatic_rerun_count": rerun_count,
            "terminal_status": observation.get("terminal_status", "absent"),
            "terminal_lifecycle_observed": observation.get(
                "terminal_lifecycle_observed"
            ),
            "connection_state_verified": observation.get(
                "connection_state_verified"
            ),
            "connection_state_error": observation.get(
                "connection_state_error",
                "",
            ),
            "runtime_reported_completion": observation.get(
                "runtime_reported_completion"
            ),
            "gameplay_observation_complete": observation.get(
                "gameplay_observation_complete"
            ),
            "gameplay_effect_observed": observation.get(
                "gameplay_effect_observed"
            ),
            "expected_gameplay_effect_verified": observation.get(
                "expected_gameplay_effect_verified"
            ),
            "partial_gameplay_effect_observed": observation.get(
                "partial_gameplay_effect_observed"
            ),
            "unexpected_effect_observed": observation.get(
                "unexpected_effect_observed"
            ),
            "prohibited_effect_absence_verified": observation.get(
                "prohibited_effect_absence_verified"
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
    aggregate_automatic_counts(
        batch_result,
        resubmit_count=resubmit_count,
        rerun_count=rerun_count,
    )


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}
