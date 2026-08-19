#20260819_kpopmodder: Validate complete immutable process evidence before identity classification.
from __future__ import annotations

import ntpath
from typing import Mapping

from .entrypoint_provenance import normalize_windows_path
from .process_start_time import process_start_time_utc_ticks
from .windows_command_line import parse_windows_command_line


def process_evidence_error(
    process: Mapping[str, object],
    *,
    expected_process_id: int | None = None,
) -> str:
    process_id = process.get("process_id")
    parent_process_id = process.get("parent_process_id")
    name = _required_text(process.get("name")).lower()
    creation_date = _required_text(process.get("creation_date"))
    executable_path = normalize_windows_path(process.get("executable_path"))
    arguments = parse_windows_command_line(process.get("command_line"))
    if type(process_id) is not int or process_id <= 0:
        return "process ID is missing or invalid"
    if expected_process_id is not None and process_id != expected_process_id:
        return "process ID does not match the requested owner"
    if type(parent_process_id) is not int or parent_process_id < 0:
        return "parent process ID is missing or invalid"
    if not name:
        return "process name is missing"
    if not creation_date or process_start_time_utc_ticks(process) is None:
        return "process start time is missing or invalid"
    if not executable_path or not ntpath.isabs(executable_path):
        return "process executable path is missing or invalid"
    if ntpath.basename(executable_path).lower() != name:
        return "process name and executable path disagree"
    if not arguments:
        return "process command line is missing or malformed"
    return ""


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""
