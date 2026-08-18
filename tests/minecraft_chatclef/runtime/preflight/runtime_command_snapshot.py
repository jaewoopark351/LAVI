#20260818_kpopmodder: Extract only the command snapshot from a Fabric ChatClef status payload.
from __future__ import annotations

from typing import Mapping

from .runtime_bridge_snapshot import runtime_bridge_snapshot


def command_snapshot(payload: object) -> dict[str, object]:
    bridge, bridge_error = runtime_bridge_snapshot(payload)
    if bridge_error:
        return {}
    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return {}
    commands = details.get("commands")
    return dict(commands) if isinstance(commands, Mapping) else {}
