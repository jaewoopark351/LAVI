#20260819_kpopmodder: Bind the listener owner and any approved launcher ancestor into one identity.
from __future__ import annotations

import ntpath
from typing import Mapping

from .approved_launcher_ancestry import find_approved_launcher_ancestor
from .entrypoint_provenance import (
    inspect_script_operand,
    normalize_windows_path,
    normalized_repository_root,
)
from .process_identity_key import process_identity_fingerprint
from .process_evidence import process_evidence_error
from .process_start_time import process_start_time_utc_ticks
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
    if (
        not invocation.get("ok")
        or invocation.get("mode") != "python_script"
        or invocation.get("executable_name") != "python.exe"
        or invocation.get("strict_script_form") is not True
    ):
        return _failure("listener owner is not an approved Python script entrypoint")
    script = inspect_script_operand(invocation.get("operand"), root)
    if not script.get("ok"):
        return _failure(str(script.get("reason") or "script entrypoint is invalid"))

    approved_ancestor, ancestry_error = find_approved_launcher_ancestor(
        _parent_process_id(owner),
        processes,
        root,
        owner,
    )
    if ancestry_error:
        return _failure(f"listener ancestor identity is invalid: {ancestry_error}")

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
    evidence_error = process_evidence_error(
        process,
        expected_process_id=expected_process_id,
    )
    if evidence_error:
        return _failure(f"listener owner identity evidence is incomplete: {evidence_error}")
    process_id = _process_id(process.get("process_id"))
    parent_process_id = _parent_process_id(process)
    creation_date = _required_text(process.get("creation_date"))
    creation_time_utc_ticks = process_start_time_utc_ticks(process)
    executable_path = normalize_windows_path(process.get("executable_path"))
    if (
        process_id != expected_process_id
        or parent_process_id < 0
        or not creation_date
        or creation_time_utc_ticks is None
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
            "intended_lavi_creation_time_utc_ticks": creation_time_utc_ticks,
            "intended_lavi_executable_path": executable_path,
        },
    }


def _parent_process_id(process: Mapping[str, object]) -> int:
    value = process.get("parent_process_id")
    return value if type(value) is int and value >= 0 else -1


def _process_id(value: object) -> int:
    return value if type(value) is int and value > 0 else -1


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "entrypoint": "", "observed": {}}
