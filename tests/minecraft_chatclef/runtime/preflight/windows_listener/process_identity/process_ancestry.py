#20260819_kpopmodder: Bind the listener owner and any approved launcher ancestor into one identity.
from __future__ import annotations

import ntpath
from typing import Mapping

from .entrypoint_provenance import (
    inspect_approved_launcher,
    inspect_script_operand,
    normalize_windows_path,
    normalized_repository_root,
)
from .process_identity_key import process_identity_fingerprint
from .python_invocation import inspect_python_invocation


def inspect_approved_process_identity(
    process_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> dict[str, object]:
    root = normalized_repository_root(repository_root)
    if type(process_id) is not int or process_id <= 0 or not root:
        return _failure("process or repository identity is invalid")
    owner = processes.get(process_id)
    if owner is None:
        return _failure("listener owner process evidence is missing")
    owner_identity = _owner_identity(owner, process_id)
    if not owner_identity.get("ok"):
        return _failure(str(owner_identity.get("reason") or "owner identity is invalid"))
    invocation = inspect_python_invocation(owner)
    if not invocation.get("ok") or invocation.get("mode") != "python_script":
        return _failure("listener owner is not an approved Python script entrypoint")
    script = inspect_script_operand(invocation.get("operand"), root)
    if not script.get("ok"):
        return _failure(str(script.get("reason") or "script entrypoint is invalid"))

    approved_ancestor = _approved_launcher_ancestor(
        _parent_process_id(owner),
        processes,
        root,
    )
    if script.get("relative") and approved_ancestor is None:
        return _failure("relative main.py lacks approved launcher entrypoint provenance")

    observed = {
        **dict(owner_identity["observed"]),
        "process_invocation_mode": "python_script",
        "process_entrypoint": "main.py",
        "resolved_entrypoint_path": script["resolved_entrypoint_path"],
        "entrypoint_provenance": script["entrypoint_provenance"],
        "repository_root": root,
        "approved_ancestor": approved_ancestor,
    }
    fingerprint = process_identity_fingerprint(observed)
    if not fingerprint:
        return _failure("listener process identity evidence is incomplete")
    observed["process_identity_fingerprint"] = fingerprint
    return {
        "ok": True,
        "reason": "approved LAVI process identity",
        "entrypoint": "main.py",
        "observed": observed,
    }


def _owner_identity(
    process: Mapping[str, object],
    expected_process_id: int,
) -> dict[str, object]:
    process_id = _process_id(process.get("process_id"))
    parent_process_id = _parent_process_id(process)
    creation_date = _required_text(process.get("creation_date"))
    executable_path = normalize_windows_path(process.get("executable_path"))
    if (
        process_id != expected_process_id
        or parent_process_id < 0
        or not creation_date
        or not executable_path
        or not ntpath.isabs(executable_path)
    ):
        return _failure("listener owner identity evidence is incomplete")
    return {
        "ok": True,
        "observed": {
            "intended_lavi_pid": process_id,
            "intended_lavi_parent_process_id": parent_process_id,
            "intended_lavi_creation_date": creation_date,
            "intended_lavi_executable_path": executable_path,
        },
    }


def _approved_launcher_ancestor(
    first_parent_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> dict[str, object] | None:
    current = first_parent_id
    seen: set[int] = set()
    for _depth in range(10):
        if current <= 0 or current in seen:
            return None
        seen.add(current)
        process = processes.get(current)
        if process is None or _process_id(process.get("process_id")) != current:
            return None
        launcher = inspect_approved_launcher(process, repository_root)
        if launcher.get("ok"):
            observed = launcher.get("observed")
            return dict(observed) if isinstance(observed, Mapping) else None
        current = _parent_process_id(process)
    return None


def _process_id(value: object) -> int:
    return value if type(value) is int and value > 0 else -1


def _parent_process_id(process: Mapping[str, object]) -> int:
    value = process.get("parent_process_id")
    return value if type(value) is int and value >= 0 else -1


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "entrypoint": "", "observed": {}}
