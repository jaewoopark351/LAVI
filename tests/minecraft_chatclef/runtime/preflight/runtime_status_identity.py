#20260818_kpopmodder: Validate Fabric ChatClef backend, connection, idle, and identity status.
from __future__ import annotations

from typing import Mapping


def inspect_runtime_status(
    payload: object,
    *,
    expected_backend: str,
) -> dict[str, object]:
    if not isinstance(payload, Mapping):
        return _failure("runtime status must be an object")
    bridge = _bridge(payload)
    if not bridge:
        return _failure("Fabric ChatClef bridge status is missing")
    backend = _text(bridge.get("backend_id"))
    connected = bridge.get("connected") is True
    lifecycle_state = _text(bridge.get("lifecycle_state"))
    enabled = bridge.get("enabled") is True
    commands = _commands(bridge)
    if commands is None or "active_request_id" not in commands:
        return _failure("Fabric ChatClef command status is incomplete")
    active_request_value = commands["active_request_id"]
    if active_request_value is None:
        active_request_id = ""
    elif isinstance(active_request_value, str) and active_request_value.strip():
        active_request_id = active_request_value.strip()
    else:
        return _failure("Fabric ChatClef active request status is invalid")
    observed = {
        "backend": backend,
        "enabled": enabled,
        "connected": connected,
        "lifecycle_state": lifecycle_state,
        "active_request_id": active_request_id or None,
        "instance": _identity_value(bridge, "instance"),
        "world": _identity_value(bridge, "world"),
    }
    if backend != expected_backend:
        return _failure("runtime backend mismatch", observed)
    if not enabled:
        return _failure("Fabric ChatClef bridge is disabled", observed)
    if not connected:
        return _failure("Fabric ChatClef bridge is disconnected", observed)
    if lifecycle_state != "connected":
        return _failure("Fabric ChatClef lifecycle is not connected", observed)
    if active_request_id:
        return _failure("Fabric ChatClef command is already active", observed)
    return {"ok": True, "reason": "runtime_status_validated", "observed": observed}


def _bridge(payload: Mapping[str, object]) -> dict[str, object]:
    details = payload.get("details")
    if isinstance(details, Mapping):
        return dict(details)
    if "backend_id" in payload:
        return dict(payload)
    return {}


def _commands(bridge: Mapping[str, object]) -> dict[str, object] | None:
    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return None
    commands = details.get("commands")
    return dict(commands) if isinstance(commands, Mapping) else None


def _identity_value(bridge: Mapping[str, object], field: str) -> str:
    direct = _text(bridge.get(field))
    if direct:
        return direct
    details = bridge.get("details")
    if isinstance(details, Mapping):
        return _text(details.get(field))
    return ""


def _failure(
    reason: str,
    observed: Mapping[str, object] | None = None,
) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": dict(observed or {})}


def _text(value: object) -> str:
    return str(value or "").strip()
