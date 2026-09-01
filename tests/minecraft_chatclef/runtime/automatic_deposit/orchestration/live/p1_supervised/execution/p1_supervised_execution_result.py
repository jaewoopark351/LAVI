#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import hashlib
import json
from collections.abc import Mapping
from dataclasses import dataclass, field


_P1_SUPERVISED_EXECUTION_RESULT_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1SupervisedExecutionResult:
    preflight_status: str
    submission_outcome: str
    submit_call_count: int
    adapter_command_request_count: int | str | None
    automatic_resubmit_count: int
    automatic_rerun_count: int
    submitted_request_id: str
    connection_state_verified: bool | None
    connection_state_error: str | None
    terminal_lifecycle_observed: bool | None
    terminal_request_id: str
    terminal_status: str
    active_request_clear: bool | None
    active_clear_observation: str
    observer_timeout: bool | None
    runtime_reported_completion: bool | None
    gameplay_observation_complete: bool | None
    gameplay_effect_observed: bool | None
    expected_gameplay_effect_verified: bool | None
    partial_gameplay_effect_observed: bool | None
    unexpected_effect_observed: bool | None
    prohibited_effect_absence_verified: bool | None
    end_to_end_success: bool | None
    reconciliation_required: bool
    result_error: str
    result_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _fingerprint(self)
        if self._seal is not _P1_SUPERVISED_EXECUTION_RESULT_SEAL:
            raise ValueError("supervised execution result must be executor-created")
        if self.result_fingerprint != expected or self._integrity != expected:
            raise ValueError("supervised execution result integrity mismatch")


def seal_p1_supervised_execution_result(
    common_result: object,
    *,
    result_error: str = "",
) -> P1SupervisedExecutionResult:
    result = common_result if isinstance(common_result, Mapping) else {}
    preflight = result.get("preflight")
    observation = result.get("observation")
    preflight_mapping = preflight if isinstance(preflight, Mapping) else {}
    observed = observation if isinstance(observation, Mapping) else {}
    values = {
        "preflight_status": _exact_text(preflight_mapping.get("status")),
        "submission_outcome": _exact_text(observed.get("submission_outcome")),
        "submit_call_count": _count(observed.get("gradio_submit_call_count")),
        "adapter_command_request_count": _adapter_request_count(
            observed.get("adapter_command_request_count")
        ),
        "automatic_resubmit_count": _count(
            observed.get("automatic_resubmit_count")
        ),
        "automatic_rerun_count": _count(observed.get("automatic_rerun_count")),
        "submitted_request_id": _identity(observed.get("submitted_request_id")),
        "connection_state_verified": _boolean(
            observed.get("connection_state_verified")
        ),
        "connection_state_error": _optional_exact_text(
            observed.get("connection_state_error")
        ),
        "terminal_lifecycle_observed": _boolean(
            observed.get("terminal_lifecycle_observed")
        ),
        "terminal_request_id": _identity(observed.get("terminal_request_id")),
        "terminal_status": _exact_text(observed.get("terminal_status")),
        "active_request_clear": _boolean(observed.get("active_request_clear")),
        "active_clear_observation": _exact_text(
            observed.get("active_clear_observation")
        ),
        "observer_timeout": _boolean(observed.get("observer_timeout")),
        "runtime_reported_completion": _boolean(
            observed.get("runtime_reported_completion")
        ),
        "gameplay_observation_complete": _boolean(
            observed.get("gameplay_observation_complete")
        ),
        "gameplay_effect_observed": _boolean(
            observed.get("gameplay_effect_observed")
        ),
        "expected_gameplay_effect_verified": _boolean(
            observed.get("expected_gameplay_effect_verified")
        ),
        "partial_gameplay_effect_observed": _boolean(
            observed.get("partial_gameplay_effect_observed")
        ),
        "unexpected_effect_observed": _boolean(
            observed.get("unexpected_effect_observed")
        ),
        "prohibited_effect_absence_verified": _boolean(
            observed.get("prohibited_effect_absence_verified")
        ),
        "end_to_end_success": _boolean(observed.get("end_to_end_success")),
        "reconciliation_required": (
            observed.get("reconciliation_required") is True
        ),
        "result_error": _text(result_error),
    }
    provisional = P1SupervisedExecutionResult.__new__(P1SupervisedExecutionResult)
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return P1SupervisedExecutionResult(
        **values,
        result_fingerprint=fingerprint,
        _seal=_P1_SUPERVISED_EXECUTION_RESULT_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(result: P1SupervisedExecutionResult) -> str:
    payload = json.dumps(
        {
            name: getattr(result, name)
            for name in (
                "preflight_status",
                "submission_outcome",
                "submit_call_count",
                "adapter_command_request_count",
                "automatic_resubmit_count",
                "automatic_rerun_count",
                "submitted_request_id",
                "connection_state_verified",
                "connection_state_error",
                "terminal_lifecycle_observed",
                "terminal_request_id",
                "terminal_status",
                "active_request_clear",
                "active_clear_observation",
                "observer_timeout",
                "runtime_reported_completion",
                "gameplay_observation_complete",
                "gameplay_effect_observed",
                "expected_gameplay_effect_verified",
                "partial_gameplay_effect_observed",
                "unexpected_effect_observed",
                "prohibited_effect_absence_verified",
                "end_to_end_success",
                "reconciliation_required",
                "result_error",
            )
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _count(value: object) -> int:
    return value if type(value) is int and value >= 0 else -1


def _adapter_request_count(value: object) -> int | str | None:
    if type(value) is int and value >= 0:
        return value
    if value == "unknown":
        return "unknown"
    return None


def _boolean(value: object) -> bool | None:
    return value if type(value) is bool else None


def _identity(value: object) -> str:
    text = _exact_text(value)
    if not text or text != text.strip():
        return ""
    if text.casefold() in {"absent", "none", "null", "unavailable", "unknown"}:
        return ""
    return text


def _exact_text(value: object) -> str:
    return value if type(value) is str else ""


def _optional_exact_text(value: object) -> str | None:
    return value if type(value) is str else None


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""
