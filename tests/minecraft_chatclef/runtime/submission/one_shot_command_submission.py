#20260818_kpopmodder: Submit one approved command once and never infer a safe replay.
from __future__ import annotations

from typing import Mapping

from ..observation.live_run_observation import (
    new_live_run_observation,
)


def submit_command_once(gateway: object, command: str) -> dict[str, object]:
    observation = new_live_run_observation()
    try:
        payload = gateway.submit_korean_command(command)
    except Exception:
        observation["submission_outcome"] = "submission_outcome_unknown"
        observation["gradio_submit_call_count"] = _call_count(gateway)
        observation["reconciliation_required"] = True
        return observation
    observation["gradio_submit_call_count"] = _call_count(gateway)
    if not isinstance(payload, Mapping):
        observation["submission_outcome"] = "submission_outcome_unknown"
        observation["reconciliation_required"] = True
        return observation
    status = payload.get("status")
    if not isinstance(status, Mapping) or type(payload.get("ok")) is not bool:
        observation["submission_outcome"] = "submission_outcome_unknown"
        observation["reconciliation_required"] = True
        return observation
    status_payload = dict(status)
    request_id = str(status_payload.get("request_id") or "").strip()
    terminal_status = str(status_payload.get("status") or "").strip().lower()
    if not request_id or not terminal_status:
        observation["submission_outcome"] = "submission_outcome_unknown"
        observation["reconciliation_required"] = True
        return observation
    if terminal_status == "unknown":
        observation["submission_outcome"] = "submission_outcome_unknown"
        observation["submitted_request_id"] = request_id
        observation["reconciliation_required"] = True
        return observation
    if payload.get("ok") is not True or terminal_status != "accepted":
        observation["submission_outcome"] = "submit_response_not_accepted"
        observation["submitted_request_id"] = request_id
        return observation
    observation["submission_outcome"] = "accepted"
    observation["submitted_request_id"] = request_id
    return observation


def _call_count(gateway: object) -> int:
    try:
        return int(getattr(gateway, "submit_call_count", 0))
    except (TypeError, ValueError):
        return 0
