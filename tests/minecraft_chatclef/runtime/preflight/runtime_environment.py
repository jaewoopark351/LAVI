#20260818_kpopmodder: Normalize test-only live runtime environment inputs in one boundary.
from __future__ import annotations

import os
from pathlib import Path
from typing import Mapping


DEFAULT_KOREAN_COMMAND = "돌 1개 가져와줘"
DEFAULT_COMMAND_TRANSPORT = "korean"
DEFAULT_FABRIC_PORT = 4316
DEFAULT_GRADIO_RANGE_START = 47860
DEFAULT_GRADIO_RANGE_END = 47959
DEFAULT_RUNTIME_TIMEOUT_SEC = 900.0
DEFAULT_GAMEPLAY_OBSERVATION_TIMEOUT_SEC = 390.0
DEFAULT_GAMEPLAY_OBSERVATION_POLL_SEC = 2.0
DEFAULT_GAMEPLAY_SNAPSHOT_MAX_AGE_SEC = 30.0
DEFAULT_GAMEPLAY_TEST_OBJECTIVE = "get_acquisition_delta"


def load_live_runtime_environment(
    source: Mapping[str, str] | None = None,
) -> dict[str, object]:
    values = os.environ if source is None else source
    return {
        "live_opt_in": values.get("LAVI_MINECRAFT_RUNTIME_TESTS") == "1",
        "mutating_opt_in": values.get("LAVI_MINECRAFT_RUNTIME_MUTATING") == "1",
        "gradio_url": _text(values.get("LAVI_GRADIO_URL")),
        "command": _text(
            values.get("LAVI_MINECRAFT_RUNTIME_KOREAN_COMMAND")
        )
        or DEFAULT_KOREAN_COMMAND,
        "transport": _text(
            values.get("LAVI_MINECRAFT_RUNTIME_COMMAND_TRANSPORT")
        )
        or DEFAULT_COMMAND_TRANSPORT,
        "expected_backend": _text(
            values.get("LAVI_MINECRAFT_EXPECTED_BACKEND")
        ),
        "expected_instance": _text(
            values.get("LAVI_MINECRAFT_EXPECTED_INSTANCE")
        ),
        "expected_world": _text(values.get("LAVI_MINECRAFT_EXPECTED_WORLD")),
        "log_dir": _text(
            values.get("LAVI_MINECRAFT_INSTANCE_LOG_DIR")
        ),
        "invocation_id": _text(
            values.get("LAVI_MINECRAFT_RUNTIME_INVOCATION_ID")
        ),
        "approval_json": _text(
            values.get("LAVI_MINECRAFT_RUNTIME_APPROVAL_JSON")
        ),
        "timeout_sec": _positive_float(
            values.get("LAVI_MINECRAFT_RUNTIME_TIMEOUT_SEC"),
            DEFAULT_RUNTIME_TIMEOUT_SEC,
        ),
        "poll_sec": _positive_float(
            values.get("LAVI_MINECRAFT_RUNTIME_POLL_SEC"),
            2.0,
        ),
        "gameplay_observation_timeout_sec": _positive_float(
            values.get("LAVI_MINECRAFT_GAMEPLAY_OBSERVATION_TIMEOUT_SEC"),
            DEFAULT_GAMEPLAY_OBSERVATION_TIMEOUT_SEC,
        ),
        "gameplay_observation_poll_sec": _positive_float(
            values.get("LAVI_MINECRAFT_GAMEPLAY_OBSERVATION_POLL_SEC"),
            DEFAULT_GAMEPLAY_OBSERVATION_POLL_SEC,
        ),
        "gameplay_snapshot_max_age_sec": _positive_float(
            values.get("LAVI_MINECRAFT_GAMEPLAY_SNAPSHOT_MAX_AGE_SEC"),
            DEFAULT_GAMEPLAY_SNAPSHOT_MAX_AGE_SEC,
        ),
        "gameplay_test_objective": _text(
            values.get("LAVI_MINECRAFT_GAMEPLAY_TEST_OBJECTIVE")
        )
        or DEFAULT_GAMEPLAY_TEST_OBJECTIVE,
        "fabric_port": DEFAULT_FABRIC_PORT,
        "gradio_range_start": DEFAULT_GRADIO_RANGE_START,
        "gradio_range_end": DEFAULT_GRADIO_RANGE_END,
        "repository_root": str(Path(__file__).resolve().parents[4]),
    }


def _text(value: object) -> str:
    return str(value or "").strip()


def _positive_float(value: object, default: float) -> float:
    if value is None or str(value).strip() == "":
        return default
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return -1.0
    return parsed if parsed > 0 else -1.0
