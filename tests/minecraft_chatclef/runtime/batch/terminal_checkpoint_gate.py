#20260819_kpopmodder: Admit only safely correlated terminal observations to the read-only gameplay checkpoint.
from __future__ import annotations

from collections.abc import Mapping

from .batch_process_identity import batch_process_identity_key


TERMINAL_STATUSES = frozenset(
    {
        "completed",
        "rejected",
        "failed",
        "cancelled",
        "deadline_exceeded",
        "unknown",
    }
)


def terminal_checkpoint_gate_error(
    command_result: Mapping[str, object],
) -> str:
    preflight = _mapping(command_result.get("preflight"))
    observation = _mapping(command_result.get("observation"))
    if preflight.get("status") != "ok":
        return "preflight_not_ok"
    if batch_process_identity_key(command_result) is None:
        return "process_identity_not_verified"
    if observation.get("submission_outcome") != "accepted":
        return "submission_not_accepted"
    submitted_request_id = _exact_request_id(
        observation.get("submitted_request_id")
    )
    if submitted_request_id is None:
        return "submitted_request_id_invalid"
    terminal_request_id = _exact_request_id(
        observation.get("terminal_request_id")
    )
    if terminal_request_id is None:
        return "terminal_request_id_invalid"
    if submitted_request_id != terminal_request_id:
        return "terminal_request_id_mismatch"
    submit_count = observation.get("gradio_submit_call_count")
    if type(submit_count) is not int or submit_count != 1:
        return "submit_call_count_violation"
    adapter_count = observation.get("adapter_command_request_count")
    if adapter_count != "unknown" and (
        type(adapter_count) is not int or adapter_count != 1
    ):
        return "adapter_command_request_count_violation"
    automatic_resubmit_count = observation.get("automatic_resubmit_count")
    if type(automatic_resubmit_count) is not int or automatic_resubmit_count != 0:
        return "automatic_resubmit_violation"
    automatic_rerun_count = observation.get("automatic_rerun_count")
    if type(automatic_rerun_count) is not int or automatic_rerun_count != 0:
        return "automatic_rerun_violation"
    if observation.get("observer_timeout") is not False:
        return "observer_timeout"
    if observation.get("connection_state_verified") is not True:
        return "runtime_connection_not_verified"
    if observation.get("terminal_lifecycle_observed") is not True:
        return "terminal_lifecycle_not_observed"
    terminal_status = observation.get("terminal_status")
    if type(terminal_status) is not str or terminal_status not in TERMINAL_STATUSES:
        return "terminal_status_invalid"
    if observation.get("active_request_clear") is not True:
        return "active_request_not_clear"
    if observation.get("active_clear_observation") != "same_snapshot":
        return "active_clear_not_same_snapshot"
    return ""


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}


def _exact_request_id(value: object) -> str | None:
    if (
        type(value) is not str
        or not value
        or value != value.strip()
        or value in {"absent", "unknown"}
    ):
        return None
    return value
