#20260818_kpopmodder: Validate one listener-owner ancestry against the exact LAVI root.
from __future__ import annotations

import ntpath
import re
from typing import Mapping


ENTRYPOINT_FILES = {
    "main.py": "main.py",
    "run.bat": "run.bat",
    "run_lav_dev.cmd": "run_lav_dev.cmd",
}


def approved_entrypoint(
    process_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> str:
    root = _normalize_windows_path(repository_root)
    if not root or not ntpath.isabs(root):
        return ""
    current = process_id
    seen: set[int] = set()
    for _depth in range(10):
        if current <= 0 or current in seen:
            break
        seen.add(current)
        process = processes.get(current)
        if process is None:
            break
        entrypoint = _process_entrypoint(process, root)
        if entrypoint:
            return entrypoint
        current = _int_value(process.get("parent_process_id"))
    return ""


def _process_entrypoint(
    process: Mapping[str, object],
    root: str,
) -> str:
    command_line = str(process.get("command_line") or "").strip()
    executable_path = str(process.get("executable_path") or "").strip()
    tokens = _command_tokens(command_line)
    path_candidates = [executable_path, *tokens]
    repository_evidence = any(
        _is_within_root(candidate, root) for candidate in path_candidates
    )
    if not repository_evidence:
        return ""

    for token in tokens:
        cleaned = _clean_token(token)
        base_name = ntpath.basename(cleaned).lower()
        label = ENTRYPOINT_FILES.get(base_name)
        if label is None:
            continue
        if ntpath.isabs(cleaned) and not _is_within_root(cleaned, root):
            continue
        return label

    lowered = [_clean_token(token).lower() for token in tokens]
    for index, token in enumerate(lowered[:-1]):
        if token == "-m" and lowered[index + 1] == "lavi":
            return "python -m lavi"
    return ""


def _command_tokens(command_line: str) -> list[str]:
    return [
        next(group for group in match.groups() if group is not None)
        for match in re.finditer(r'"([^"]*)"|\'([^\']*)\'|(\S+)', command_line)
    ]


def _clean_token(value: str) -> str:
    return str(value or "").strip().strip('"\'').rstrip(",;")


def _is_within_root(candidate: str, root: str) -> bool:
    cleaned = _clean_token(candidate)
    if not cleaned or not ntpath.isabs(cleaned):
        return False
    normalized = _normalize_windows_path(cleaned)
    try:
        return ntpath.commonpath((root, normalized)) == root
    except ValueError:
        return False


def _normalize_windows_path(value: str) -> str:
    return ntpath.normcase(ntpath.normpath(str(value or "").replace("/", "\\")))


def _int_value(value: object) -> int:
    if isinstance(value, bool):
        return -1
    try:
        return int(value)
    except (TypeError, ValueError):
        return -1
