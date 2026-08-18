#20260818_kpopmodder: Extract exact instance/world identity from one approved latest.log.
from __future__ import annotations

from pathlib import Path
from typing import Callable

from .minecraft_log_snapshot import (
    read_stable_log_snapshot,
)
from .minecraft_log_session_identity import (
    parse_active_session_identity,
)


def inspect_minecraft_log_identity(
    log_dir: object,
    *,
    expected_instance: str,
    expected_world: str,
    snapshot_reader: Callable[..., dict[str, object]] = read_stable_log_snapshot,
) -> dict[str, object]:
    directory_text = str(log_dir or "").strip()
    if not directory_text:
        return _failure("LAVI_MINECRAFT_INSTANCE_LOG_DIR is required")
    directory = Path(directory_text)
    if not directory.is_dir():
        return _failure("Minecraft log directory does not exist")
    if directory.name.lower() != "logs":
        return _failure("Minecraft log directory must be the exact logs directory")
    parent_instance = directory.parent.name
    if parent_instance != expected_instance:
        return _failure("log directory parent instance mismatch")
    latest_log = directory / "latest.log"
    snapshot = snapshot_reader(latest_log)
    if not snapshot.get("ok"):
        return _failure(str(snapshot.get("reason") or "latest.log snapshot failed"))
    identity = parse_active_session_identity(
        str(snapshot.get("text") or ""),
        final_line_complete=bool(snapshot.get("final_line_complete")),
    )
    if not identity.get("ok"):
        return _failure(str(identity.get("reason") or "active session identity failed"))
    observed_instance = str(identity["instance"])
    observed_world = str(identity["world"])
    observed = {
        "instance": observed_instance,
        "world": observed_world,
        "identity_source": "minecraft_latest_log",
        "log_encoding": snapshot.get("encoding"),
        "log_snapshot_stable": True,
    }
    if observed_instance != expected_instance:
        return _failure("observed Minecraft instance mismatch", observed)
    if observed_world != expected_world:
        return _failure("observed Minecraft world mismatch", observed)
    return {"ok": True, "reason": "minecraft_log_identity_validated", "observed": observed}


def _failure(
    reason: str,
    observed: dict[str, object] | None = None,
) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": dict(observed or {})}
