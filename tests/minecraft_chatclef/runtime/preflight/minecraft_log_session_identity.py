#20260818_kpopmodder: Parse only the final active integrated-server session identity.
from __future__ import annotations

import re


_START_MARKER = "Starting integrated minecraft server version"
_STOP_MARKERS = ("Stopping server", "Saving worlds")
_INSTANCE_WORLD_RE = re.compile(
    r"(?i)[\\/]Instances[\\/](?P<instance>[^\\/]+)"
    r"[\\/]saves[\\/](?P<world>[^\\/]+)[\\/]"
)


def parse_active_session_identity(
    text: str,
    *,
    final_line_complete: bool,
) -> dict[str, object]:
    lines = text.splitlines()
    if not final_line_complete and lines:
        lines = lines[:-1]
    start_indexes = [
        index for index, line in enumerate(lines) if _START_MARKER in line
    ]
    if not start_indexes:
        return _failure("integrated server start marker is missing")
    active_segment = lines[start_indexes[-1] :]
    if any(marker in line for line in active_segment for marker in _STOP_MARKERS):
        return _failure("latest integrated server session is stopped or stopping")
    identities = {
        (match.group("instance"), match.group("world"))
        for line in active_segment
        for match in _INSTANCE_WORLD_RE.finditer(line)
    }
    if not identities:
        return _failure("active log segment has no instance/world identity")
    if len(identities) != 1:
        return _failure("active log segment has ambiguous instance/world identity")
    instance, world = next(iter(identities))
    return {"ok": True, "reason": "active_session_identity", "instance": instance, "world": world}


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "instance": "", "world": ""}
