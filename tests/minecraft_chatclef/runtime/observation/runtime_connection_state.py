#20260819_kpopmodder: Validate the bridge connection in each terminal observation snapshot.
from __future__ import annotations

from ..preflight.runtime_bridge_snapshot import runtime_bridge_snapshot


def runtime_connection_error(payload: object) -> str:
    bridge, bridge_error = runtime_bridge_snapshot(payload)
    if bridge_error:
        return bridge_error
    if bridge.get("backend_id") != "fabric_chatclef":
        return "Fabric ChatClef backend identity is invalid"
    if bridge.get("enabled") is not True:
        return "Fabric ChatClef bridge is disabled"
    if bridge.get("connected") is not True:
        return "Fabric ChatClef bridge is disconnected"
    if bridge.get("lifecycle_state") != "connected":
        return "Fabric ChatClef lifecycle is not connected"
    return ""
