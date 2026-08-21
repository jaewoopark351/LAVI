#20260820_kpopmodder: Extract accepted lifecycle evidence for dry-run active-command reconciliation.
from __future__ import annotations

from typing import Any, Mapping

from .reconciliation_mapping_utils import (
    bool_or_unknown,
    first_text,
    first_value,
    lower_text,
    mapping,
    short_text,
    text,
)


class ActiveCommandLifecycleEvidenceBuilder:
    TERMINAL_STATUSES = {
        "completed",
        "rejected",
        "failed",
        "cancelled",
        "deadline_exceeded",
        "unknown",
    }

    def __init__(self, neutral_root_classes: set[str]):
        self._neutral_root_classes = neutral_root_classes

    def build(
        self,
        commands: Mapping[str, Any],
        last_result: Mapping[str, Any],
        last_data: Mapping[str, Any],
    ) -> dict[str, Any]:
        active_request_id = text(commands.get("active_request_id"))
        last_request_id = text(last_result.get("request_id"))
        last_status = lower_text(last_result.get("status")) or "unknown"
        last_matches_active = (
            None
            if active_request_id is None or last_request_id is None
            else active_request_id == last_request_id
        )
        evidence = mapping(last_data.get("lifecycle_evidence"))
        current_root = _current_root_summary(last_data)
        bound_root = _bound_root_summary(last_data)
        stable_quiescence = _stable_quiescence_summary(last_data)
        current_root_neutral = _root_is_neutral(
            current_root,
            self._neutral_root_classes,
        )
        stronger_terminal_result_present = (
            last_matches_active is True and last_status in self.TERMINAL_STATUSES
        )
        return {
            "last_result_request_id": last_request_id,
            "last_result_matches_active": last_matches_active,
            "last_result_status": last_status,
            "last_result_ok": bool_or_unknown(last_result.get("ok")),
            "last_result_error_code": text(last_result.get("error_code")),
            "last_result_message": short_text(last_result.get("message")),
            "last_result_reason": (
                first_text(last_data, ("result_reason", "submission_outcome"))
                or "unknown"
            ),
            "lifecycle_evidence_version": first_text(
                evidence,
                ("version", "lifecycle_evidence_version"),
            )
            or text(last_data.get("lifecycle_evidence_version"))
            or "unknown",
            "lifecycle_evidence_stage": first_text(
                evidence,
                ("stage", "lifecycle_evidence_stage"),
            )
            or text(last_data.get("lifecycle_evidence_stage"))
            or first_text(last_data, ("result_reason",))
            or "unknown",
            "evidence_sequence": first_value(
                evidence,
                ("evidence_sequence", "sequence"),
            )
            if first_value(evidence, ("evidence_sequence", "sequence"))
            is not None
            else first_value(last_data, ("evidence_sequence", "sequence")),
            "classification": first_text(last_data, ("classification",))
            or "unknown",
            "terminal_status": first_text(last_data, ("terminal_status",))
            or "unknown",
            "gameplay_effect": first_text(last_data, ("gameplay_effect",))
            or first_text(evidence, ("gameplay_effect",))
            or "unknown",
            "dispatch_returned": bool_or_unknown(last_data.get("dispatch_returned")),
            "finish_callback_received": bool_or_unknown(
                last_data.get("finish_callback_received")
            ),
            "task_finished_event_received": bool_or_unknown(
                last_data.get("task_finished_event_received")
            ),
            "waiting_reason": first_text(last_data, ("waiting_reason",))
            or "unknown",
            "bound_root_task": bound_root,
            "current_root_task": current_root,
            "current_root_is_neutral": current_root_neutral,
            "request_root_observation_state": (
                text(stable_quiescence.get("request_root_observation_state"))
                or "UNKNOWN"
            ),
            "stable_request_quiescence": stable_quiescence,
            "stronger_terminal_result_present": stronger_terminal_result_present,
        }


def _task_summary(value: Any) -> dict[str, Any]:
    if isinstance(value, str):
        return {
            "class_name": text(value) or "unknown",
            "identity": "unknown",
        }
    payload = mapping(value)
    return {
        "class_name": (
            first_text(
                payload,
                (
                    "class_name",
                    "task_class",
                    "taskClass",
                    "root_class",
                    "current_task_class",
                    "user_task_root_class",
                ),
            )
            or "unknown"
        ),
        "identity": (
            first_text(
                payload,
                (
                    "identity",
                    "task_identity",
                    "taskIdentity",
                    "root_identity",
                    "current_task_identity",
                    "user_task_root_identity",
                ),
            )
            or "unknown"
        ),
    }


def _bound_root_summary(data: Mapping[str, Any]) -> dict[str, Any]:
    for key in ("bound_root_task", "bound_root", "request_bound_root"):
        summary = _task_summary(data.get(key))
        if summary["class_name"] != "unknown":
            return summary
    return {
        "class_name": first_text(
            data,
            ("bound_root_task_class", "bound_root_class"),
        )
        or "unknown",
        "identity": first_text(
            data,
            ("bound_root_task_identity", "bound_root_identity"),
        )
        or "unknown",
    }


def _current_root_summary(data: Mapping[str, Any]) -> dict[str, Any]:
    for key in (
        "current_root_task",
        "current_runtime_root",
        "current_task",
        "current_root",
    ):
        summary = _task_summary(data.get(key))
        if summary["class_name"] != "unknown":
            return summary
    ownership = mapping(data.get("ownership"))
    for key in ("current_root_task", "user_task_root", "current_task"):
        summary = _task_summary(ownership.get(key))
        if summary["class_name"] != "unknown":
            return summary
    observation = mapping(data.get("task_finished_observation"))
    for ownership_key in ("ownership_at_dequeue", "ownership_at_observe"):
        observed_ownership = mapping(observation.get(ownership_key))
        for root_key in ("user_task_root", "current_root_task", "current_task"):
            summary = _task_summary(observed_ownership.get(root_key))
            if summary["class_name"] != "unknown":
                return summary
    return {"class_name": "unknown", "identity": "unknown"}


def _root_is_neutral(
    current_root: Mapping[str, Any],
    neutral_root_classes: set[str],
) -> bool | str:
    class_name = text(current_root.get("class_name"))
    if class_name is None or class_name == "unknown":
        return "unknown"
    return class_name in neutral_root_classes


def _stable_quiescence_summary(data: Mapping[str, Any]) -> dict[str, Any]:
    source = mapping(data.get("stable_request_quiescence"))
    if not source:
        source = mapping(data.get("stable_quiescence"))
    qualified = source.get("qualified")
    if qualified is None:
        qualified = source.get("satisfied")
    observation_count = source.get("observation_count")
    if observation_count is None:
        observation_count = source.get("consecutive_neutral_snapshots")
    stable_duration_ms = source.get("stable_duration_ms")
    if stable_duration_ms is None:
        stable_duration_ms = source.get("neutral_duration_ms")
    return {
        "qualified": bool_or_unknown(qualified),
        "satisfied": bool_or_unknown(qualified),
        "blocked_reason": text(source.get("blocked_reason")),
        "elapsed_since_finish_callback_ms": source.get(
            "elapsed_since_finish_callback_ms"
        ),
        "observation_count": observation_count,
        "consecutive_neutral_snapshots": observation_count,
        "neutral_duration_ms": stable_duration_ms,
        "stable_duration_ms": stable_duration_ms,
        "snapshot_age_ms": source.get("snapshot_age_ms"),
        "same_session_generation": bool_or_unknown(
            source.get("same_session_generation")
        ),
        "request_root_reappeared": bool_or_unknown(
            source.get("request_root_reappeared")
        ),
        "request_root_observation_state": (
            text(source.get("request_root_observation_state")) or "UNKNOWN"
        ),
        "first_client_tick_id": source.get("first_client_tick_id"),
        "last_client_tick_id": source.get("last_client_tick_id"),
        "signature_version": text(source.get("signature_version")),
        "policy_version": text(source.get("policy_version")),
    }
