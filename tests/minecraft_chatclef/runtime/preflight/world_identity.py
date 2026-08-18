#20260818_kpopmodder: Resolve exact instance/world identity from status or approved log fallback.
from __future__ import annotations

from typing import Callable, Mapping


def inspect_world_identity(
    environment: Mapping[str, object],
    status_observed: Mapping[str, object],
    log_identity_inspector: Callable[..., dict[str, object]],
) -> dict[str, object]:
    status_instance = str(status_observed.get("instance") or "").strip()
    status_world = str(status_observed.get("world") or "").strip()
    expected_instance = str(environment["expected_instance"])
    expected_world = str(environment["expected_world"])
    if bool(status_instance) != bool(status_world):
        return _failure("runtime identity is partially populated")
    if status_instance and status_world:
        observed = {
            "instance": status_instance,
            "world": status_world,
            "identity_source": "runtime_status",
        }
        if status_instance != expected_instance:
            return _failure("runtime instance mismatch", observed)
        if status_world != expected_world:
            return _failure("runtime world mismatch", observed)
        return {
            "ok": True,
            "reason": "runtime_identity_validated",
            "observed": observed,
        }
    return log_identity_inspector(
        environment.get("log_dir"),
        expected_instance=expected_instance,
        expected_world=expected_world,
    )


def _failure(
    reason: str,
    observed: Mapping[str, object] | None = None,
) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": dict(observed or {})}
