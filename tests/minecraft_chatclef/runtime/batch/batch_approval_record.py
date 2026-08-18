#20260818_kpopmodder: Validate one exact operator approval for every batch step.
from __future__ import annotations

from collections.abc import Mapping, Sequence


APPROVAL_TARGET_FIELDS = (
    ("gradio_url", "gradio_url"),
    ("backend", "expected_backend"),
    ("instance", "expected_instance"),
    ("world", "expected_world"),
)


def validate_batch_approval_record(
    approval: Mapping[str, object],
    expected_commands: Sequence[str],
    environment: Mapping[str, object],
) -> tuple[list[dict[str, str]], str]:
    if not _text(approval.get("approval_source")):
        return [], "batch approval_source is required"
    if approval.get("one_shot") is not True:
        return [], "batch approval must set one_shot=true for every listed command"
    if approval.get("automatic_rerun_disabled") is not True:
        return [], "batch approval must confirm automatic_rerun_disabled=true"
    for approval_field, environment_field in APPROVAL_TARGET_FIELDS:
        approved = _text(approval.get(approval_field))
        expected = _text(environment.get(environment_field))
        if not approved:
            return [], f"batch approval field is missing: {approval_field}"
        if approved != expected:
            return [], f"batch approval field mismatch: {approval_field}"
    raw_commands = approval.get("commands")
    if isinstance(raw_commands, (str, bytes)) or not isinstance(
        raw_commands,
        Sequence,
    ):
        return [], "batch approval commands must be a sequence"
    if len(raw_commands) != len(expected_commands):
        return [], "batch approval command count does not match the fixed plan"
    normalized: list[dict[str, str]] = []
    invocation_ids: set[str] = set()
    for index, expected_command in enumerate(expected_commands):
        raw_step = raw_commands[index]
        if not isinstance(raw_step, Mapping):
            return [], f"batch approval command {index} must be an object"
        command = _text(raw_step.get("command"))
        invocation_id = _text(raw_step.get("invocation_id"))
        if command != expected_command:
            return [], f"batch approval command mismatch at index {index}"
        if not invocation_id:
            return [], f"batch invocation {index} is blank"
        if invocation_id in invocation_ids:
            return [], f"batch invocation is duplicated: {invocation_id}"
        invocation_ids.add(invocation_id)
        normalized.append({"command": command, "invocation_id": invocation_id})
    return normalized, ""


def _text(value: object) -> str:
    return str(value or "").strip()
