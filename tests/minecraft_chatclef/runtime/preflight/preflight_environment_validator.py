#20260818_kpopmodder: Validate all required live preflight inputs without side effects.
from __future__ import annotations

from typing import Mapping


REQUIRED_TEXT_FIELDS = (
    "gradio_url",
    "command",
    "expected_backend",
    "expected_instance",
    "expected_world",
    "invocation_id",
    "approval_json",
    "repository_root",
)


def validate_preflight_environment(environment: Mapping[str, object]) -> str:
    for field in REQUIRED_TEXT_FIELDS:
        if not str(environment.get(field) or "").strip():
            return f"required live runtime value is missing: {field}"
    if _positive_float(environment.get("timeout_sec")) is None:
        return "runtime timeout must be positive"
    if _positive_float(environment.get("poll_sec")) is None:
        return "runtime poll interval must be positive"
    fabric_port = _port(environment.get("fabric_port"))
    range_start = _port(environment.get("gradio_range_start"))
    range_end = _port(environment.get("gradio_range_end"))
    if fabric_port is None:
        return "Fabric port must be between 1 and 65535"
    if range_start is None or range_end is None or range_start > range_end:
        return "Gradio fallback port range is invalid"
    return ""


def _positive_float(value: object) -> float | None:
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if parsed > 0 else None


def _port(value: object) -> int | None:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        return None
    return parsed if 1 <= parsed <= 65535 else None
