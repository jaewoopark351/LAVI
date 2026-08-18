#20260819_kpopmodder: Validate exact repository script and launcher provenance for mutating preflight.
from __future__ import annotations

import ntpath
from typing import Mapping

from .windows_command_line import parse_windows_command_line


APPROVED_LAUNCHERS = ("run.bat", "run_lav_dev.cmd")


def normalize_windows_path(value: object) -> str:
    if type(value) is not str or not value.strip():
        return ""
    return ntpath.normcase(ntpath.normpath(value.strip().replace("/", "\\")))


def normalized_repository_root(value: object) -> str:
    root = normalize_windows_path(value)
    return root if root and ntpath.isabs(root) else ""


def inspect_script_operand(operand: object, repository_root: str) -> dict[str, object]:
    if type(operand) is not str or not operand.strip():
        return _failure("script operand is missing")
    expected = ntpath.join(repository_root, "main.py")
    cleaned = operand.strip()
    if ntpath.isabs(cleaned):
        resolved = normalize_windows_path(cleaned)
        if resolved != expected:
            return _failure("script operand is not the exact repository entrypoint")
        return {
            "ok": True,
            "relative": False,
            "resolved_entrypoint_path": expected,
            "entrypoint_provenance": "exact_repository_script",
        }
    if normalize_windows_path(cleaned) != "main.py":
        return _failure("relative script operand is not main.py")
    return {
        "ok": True,
        "relative": True,
        "resolved_entrypoint_path": expected,
        "entrypoint_provenance": "approved_launcher_relative_script",
    }


def inspect_approved_launcher(
    process: Mapping[str, object],
    repository_root: str,
) -> dict[str, object]:
    process_id = _exact_process_id(process.get("process_id"))
    parent_process_id = _exact_parent_process_id(process.get("parent_process_id"))
    creation_date = _required_text(process.get("creation_date"))
    executable_path = normalize_windows_path(process.get("executable_path"))
    if (
        process_id <= 0
        or parent_process_id < 0
        or not creation_date
        or not executable_path
        or not ntpath.isabs(executable_path)
        or ntpath.basename(executable_path).lower() not in {"cmd", "cmd.exe"}
    ):
        return _failure("launcher process identity is incomplete")
    arguments = parse_windows_command_line(process.get("command_line"))
    if not arguments or ntpath.basename(arguments[0]).lower() not in {"cmd", "cmd.exe"}:
        return _failure("launcher command line is unavailable or malformed")
    command_index = _cmd_command_index(arguments)
    if command_index < 0 or command_index >= len(arguments):
        return _failure("launcher is not the cmd command operand")
    launcher_path = normalize_windows_path(arguments[command_index])
    approved_paths = {
        ntpath.join(repository_root, launcher_name) for launcher_name in APPROVED_LAUNCHERS
    }
    if launcher_path not in approved_paths:
        return _failure("launcher operand is not an exact approved repository launcher")
    return {
        "ok": True,
        "observed": {
            "process_id": process_id,
            "parent_process_id": parent_process_id,
            "creation_date": creation_date,
            "executable_path": executable_path,
            "invocation_mode": "cmd_launcher",
            "resolved_entrypoint_path": launcher_path,
            "entrypoint_provenance": "exact_repository_launcher",
        },
    }


def _cmd_command_index(arguments: tuple[str, ...]) -> int:
    index = 1
    while index < len(arguments):
        lowered = arguments[index].lower()
        if lowered in {"/d", "/q", "/s"}:
            index += 1
            continue
        if lowered in {"/c", "/k"}:
            return index + 1
        return -1
    return -1


def _exact_process_id(value: object) -> int:
    return value if type(value) is int and value > 0 else -1


def _exact_parent_process_id(value: object) -> int:
    return value if type(value) is int and value >= 0 else -1


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": {}}
