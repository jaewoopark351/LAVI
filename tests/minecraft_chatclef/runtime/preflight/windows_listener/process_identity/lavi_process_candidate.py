#20260819_kpopmodder: Classify conservative second-LAVI candidates separately from approval.
from __future__ import annotations

import ntpath
from typing import Mapping

from .approved_launcher_ancestry import find_approved_launcher_ancestor
from .entrypoint_provenance import normalize_windows_path, normalized_repository_root
from .process_evidence import process_evidence_error
from .python_invocation import (
    PYTHON_PROCESS_NAMES,
    inspect_python_invocation,
)
from .windows_command_line import parse_windows_command_line


LAVI_PROCESS_CANDIDATE = "lavi"
UNRELATED_PROCESS = "unrelated"
AMBIGUOUS_PROCESS = "ambiguous"


def classify_lavi_process_candidate(
    process_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> str:
    process = processes.get(process_id)
    if process is None:
        return AMBIGUOUS_PROCESS
    root = normalized_repository_root(repository_root)
    executable_path = normalize_windows_path(process.get("executable_path"))
    executable_name = ntpath.basename(executable_path).lower()
    reported_name = _text(process.get("name")).lower()
    looks_python = (
        executable_name in PYTHON_PROCESS_NAMES
        or reported_name in PYTHON_PROCESS_NAMES
    )
    if root and _is_within(executable_path, root):
        return LAVI_PROCESS_CANDIDATE
    arguments = parse_windows_command_line(process.get("command_line"))
    if _arguments_show_lavi_candidate(arguments, root):
        return LAVI_PROCESS_CANDIDATE

    evidence_error = process_evidence_error(
        process,
        expected_process_id=process_id,
    )
    if evidence_error:
        return AMBIGUOUS_PROCESS
    if not looks_python:
        return UNRELATED_PROCESS

    invocation = inspect_python_invocation(process)
    if invocation.get("ok"):
        mode = invocation.get("mode")
        operand = _text(invocation.get("operand"))
        if mode == "python_module" and _is_lavi_module(operand):
            return LAVI_PROCESS_CANDIDATE
        if mode == "python_script" and ntpath.basename(operand).lower() == "main.py":
            return LAVI_PROCESS_CANDIDATE
    else:
        return AMBIGUOUS_PROCESS

    approved_ancestor, ancestry_error = find_approved_launcher_ancestor(
        _parent_process_id(process),
        processes,
        root,
        process,
    )
    if approved_ancestor is not None:
        return LAVI_PROCESS_CANDIDATE
    if ancestry_error:
        return AMBIGUOUS_PROCESS
    return UNRELATED_PROCESS


def _arguments_show_lavi_candidate(
    arguments: tuple[str, ...],
    root: str,
) -> bool:
    for index, argument in enumerate(arguments[1:], start=1):
        lowered = argument.lower()
        if lowered == "-m" and index + 1 < len(arguments):
            if _is_lavi_module(arguments[index + 1]):
                return True
        if lowered.startswith("-m") and _is_lavi_module(argument[2:]):
            return True
        normalized = normalize_windows_path(argument)
        if ntpath.basename(normalized).lower() == "main.py":
            return True
        if root and ntpath.isabs(normalized) and _is_within(normalized, root):
            return True
    return False


def _is_lavi_module(value: object) -> bool:
    module = _text(value).lower()
    return module == "lavi" or module.startswith("lavi.")


def _is_within(path: str, root: str) -> bool:
    if not path or not root:
        return False
    try:
        return ntpath.commonpath((path, root)) == root
    except ValueError:
        return False


def _parent_process_id(process: Mapping[str, object]) -> int:
    value = process.get("parent_process_id")
    return value if type(value) is int and value >= 0 else -1


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""
