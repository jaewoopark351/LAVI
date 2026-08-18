#20260818_kpopmodder: Validate all required live preflight inputs without side effects.
from __future__ import annotations

import math
from typing import Mapping

from .windows_listener.process_identity.expected_process_identity import (
    expected_process_identity_fingerprint,
)


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
        if not _exact_text(environment.get(field)):
            return f"required live runtime value is missing or invalid: {field}"
    _expected_process_identity, identity_error = (
        expected_process_identity_fingerprint(environment)
    )
    if identity_error:
        return identity_error
    if _positive_number(environment.get("timeout_sec")) is None:
        return "runtime timeout must be positive"
    if _positive_number(environment.get("poll_sec")) is None:
        return "runtime poll interval must be positive"
    fabric_port = _port(environment.get("fabric_port"))
    range_start = _port(environment.get("gradio_range_start"))
    range_end = _port(environment.get("gradio_range_end"))
    if fabric_port is None:
        return "Fabric port must be between 1 and 65535"
    if range_start is None or range_end is None or range_start > range_end:
        return "Gradio fallback port range is invalid"
    return ""


def _exact_text(value: object) -> str:
    if type(value) is not str or not value or value != value.strip():
        return ""
    return value


def _positive_number(value: object) -> float | None:
    if type(value) not in {int, float}:
        return None
    parsed = float(value)
    return parsed if math.isfinite(parsed) and parsed > 0 else None


def _port(value: object) -> int | None:
    if type(value) is not int:
        return None
    return value if 1 <= value <= 65535 else None
