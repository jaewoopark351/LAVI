#20260818_kpopmodder: Extract only the command snapshot from a Fabric ChatClef status payload.
from __future__ import annotations

from typing import Mapping


def command_snapshot(payload: object) -> dict[str, object]:
    if not isinstance(payload, Mapping):
        return {}
    bridge = _bridge(payload)
    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return {}
    commands = details.get("commands")
    return dict(commands) if isinstance(commands, Mapping) else {}


def _bridge(payload: Mapping[str, object]) -> dict[str, object]:
    details = payload.get("details")
    if isinstance(details, Mapping):
        return dict(details)
    if "backend_id" in payload:
        return dict(payload)
    return {}
