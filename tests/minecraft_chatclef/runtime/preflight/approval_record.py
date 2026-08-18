#20260818_kpopmodder: Validate a fresh exact-tuple approval record separately from opt-in flags.
from __future__ import annotations

import json
from typing import Mapping


APPROVAL_FIELDS = (
    "command",
    "gradio_url",
    "backend",
    "instance",
    "world",
    "invocation_id",
)


def parse_approval_record(raw_json: object) -> tuple[dict[str, object], str]:
    if not isinstance(raw_json, str) or not raw_json.strip():
        return {}, "approval record is required"
    try:
        payload = json.loads(raw_json)
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        return {}, f"approval record is invalid JSON: {type(error).__name__}"
    if not isinstance(payload, Mapping):
        return {}, "approval record must be a JSON object"
    return dict(payload), ""


def validate_approval_record(
    approval: Mapping[str, object],
    expected: Mapping[str, object],
) -> str:
    for field in APPROVAL_FIELDS:
        approved = _text(approval.get(field))
        required = _text(expected.get(field))
        if not approved:
            return f"approval field is missing: {field}"
        if approved != required:
            return f"approval field mismatch: {field}"
    if not _text(approval.get("approval_source")):
        return "approval_source is required"
    if approval.get("one_shot") is not True:
        return "approval must explicitly set one_shot=true"
    if approval.get("automatic_rerun_disabled") is not True:
        return "approval must confirm automatic_rerun_disabled=true"
    return ""


def _text(value: object) -> str:
    return str(value or "").strip()
