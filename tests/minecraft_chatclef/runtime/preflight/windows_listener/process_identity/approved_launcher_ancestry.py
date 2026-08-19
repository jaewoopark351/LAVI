#20260819_kpopmodder: Isolate approved launcher ancestry and start-time ordering.
from __future__ import annotations

import ntpath
from typing import Mapping

from .entrypoint_provenance import (
    APPROVED_LAUNCHERS,
    inspect_approved_launcher,
    normalize_windows_path,
)
from .process_evidence import process_evidence_error
from .process_start_time import process_started_no_later_than
from .windows_command_line import parse_windows_command_line


def find_approved_launcher_ancestor(
    first_parent_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
    child_process: Mapping[str, object],
) -> tuple[dict[str, object] | None, str]:
    current = first_parent_id
    child = child_process
    seen: set[int] = set()
    for _depth in range(10):
        if current == 0:
            return None, ""
        if current < 0:
            return None, "ancestor parent process ID is invalid"
        if current in seen:
            return None, "process ancestry contains a cycle"
        seen.add(current)
        process = processes.get(current)
        if process is None:
            return None, "ancestor process evidence is missing"
        evidence_error = process_evidence_error(
            process,
            expected_process_id=current,
        )
        if evidence_error:
            return None, f"ancestor process evidence is incomplete: {evidence_error}"
        if not process_started_no_later_than(process, child):
            return None, "ancestor process start time is unavailable or later than child"
        launcher = inspect_approved_launcher(process, repository_root)
        if launcher.get("ok"):
            observed = launcher.get("observed")
            if not isinstance(observed, Mapping):
                return None, "approved launcher identity is unavailable"
            return dict(observed), ""
        executable_name = ntpath.basename(
            normalize_windows_path(process.get("executable_path"))
        ).lower()
        if executable_name == "cmd.exe" and _claims_repository_launcher(
            process,
            repository_root,
        ):
            return None, str(
                launcher.get("reason")
                or "command processor ancestor is not an approved launcher"
            )
        child = process
        current = _parent_process_id(process)
    return None, "approved launcher ancestor depth exceeded"


def _parent_process_id(process: Mapping[str, object]) -> int:
    value = process.get("parent_process_id")
    return value if type(value) is int and value >= 0 else -1


def _claims_repository_launcher(
    process: Mapping[str, object],
    repository_root: str,
) -> bool:
    approved_paths = {
        ntpath.join(repository_root, launcher_name)
        for launcher_name in APPROVED_LAUNCHERS
    }
    return any(
        normalize_windows_path(argument) in approved_paths
        for argument in parse_windows_command_line(process.get("command_line"))[1:]
    )
