#20260818_kpopmodder: Create one neutral live-run observation record.
from __future__ import annotations


def new_live_run_observation() -> dict[str, object]:
    return {
        "submission_outcome": "not_attempted",
        "gradio_submit_call_count": 0,
        "adapter_command_request_count": "unknown",
        "automatic_resubmit_count": 0,
        "automatic_rerun_count": 0,
        "submitted_request_id": "absent",
        "terminal_lifecycle_observed": None,
        "terminal_request_id": "absent",
        "terminal_status": "absent",
        "active_request_clear": None,
        "active_clear_observation": "not_observed",
        "observer_timeout": False,
        "runtime_reported_completion": None,
        "gameplay_observation_complete": None,
        "gameplay_effect_observed": None,
        "expected_gameplay_effect_verified": None,
        "partial_gameplay_effect_observed": None,
        "unexpected_effect_observed": None,
        "prohibited_effect_absence_verified": None,
        "end_to_end_success": None,
        "reconciliation_required": False,
    }
