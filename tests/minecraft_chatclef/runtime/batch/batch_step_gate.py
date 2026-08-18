#20260818_kpopmodder: Decide whether one observed command may advance the live batch.
from __future__ import annotations

from collections.abc import Mapping


def batch_step_gate_error(command_result: Mapping[str, object]) -> str:
    preflight = _mapping(command_result.get("preflight"))
    observation = _mapping(command_result.get("observation"))
    if preflight.get("status") != "ok":
        return "preflight_not_ok"
    if observation.get("submission_outcome") != "accepted":
        return "submission_not_accepted"
    if observation.get("gradio_submit_call_count") != 1:
        return "submit_call_count_violation"
    adapter_count = observation.get("adapter_command_request_count")
    if isinstance(adapter_count, int) and adapter_count != 1:
        return "adapter_command_request_count_violation"
    automatic_resubmit_count = observation.get("automatic_resubmit_count")
    if type(automatic_resubmit_count) is not int or automatic_resubmit_count != 0:
        return "automatic_resubmit_violation"
    automatic_rerun_count = observation.get("automatic_rerun_count")
    if type(automatic_rerun_count) is not int or automatic_rerun_count != 0:
        return "automatic_rerun_violation"
    if observation.get("observer_timeout") is True:
        return "observer_timeout"
    if observation.get("terminal_lifecycle_observed") is not True:
        return "terminal_lifecycle_not_observed"
    if observation.get("active_request_clear") is not True:
        return "active_request_not_clear"
    if observation.get("active_clear_observation") != "same_snapshot":
        return "active_clear_not_same_snapshot"
    if observation.get("terminal_status") != "completed":
        return "terminal_status_not_completed"
    if observation.get("runtime_reported_completion") is not True:
        return "runtime_completion_not_verified"
    return ""


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}
