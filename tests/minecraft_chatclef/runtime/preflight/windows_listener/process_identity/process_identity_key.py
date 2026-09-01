#20260819_kpopmodder: Build and validate one canonical process identity for TOCTOU comparison.
from __future__ import annotations

import hashlib
import json
import ntpath
from typing import Mapping

from .entrypoint_provenance import (
    is_approved_command_processor_path,
    normalize_windows_path,
)


def process_identity_fingerprint(evidence: Mapping[str, object]) -> str:
    canonical = _canonical_process_identity(evidence)
    if canonical is None:
        return ""
    encoded = json.dumps(
        canonical,
        ensure_ascii=True,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def process_identity_key(evidence: Mapping[str, object]) -> str | None:
    expected = evidence.get("process_identity_fingerprint")
    if type(expected) is not str or not expected.strip():
        return None
    actual = process_identity_fingerprint(evidence)
    if not actual or expected.strip().lower() != actual:
        return None
    return actual


def _canonical_process_identity(
    evidence: Mapping[str, object],
) -> dict[str, object] | None:
    process_id = _positive_int(evidence.get("intended_lavi_pid"))
    parent_process_id = _nonnegative_int(
        evidence.get("intended_lavi_parent_process_id")
    )
    creation_date = _required_text(evidence.get("intended_lavi_creation_date"))
    creation_time_utc_ticks = _positive_int(
        evidence.get("intended_lavi_creation_time_utc_ticks")
    )
    executable_path = _absolute_path(evidence.get("intended_lavi_executable_path"))
    invocation_mode = _required_text(evidence.get("process_invocation_mode"))
    process_entrypoint = _required_text(evidence.get("process_entrypoint"))
    resolved_entrypoint_path = _absolute_path(
        evidence.get("resolved_entrypoint_path")
    )
    provenance = _required_text(evidence.get("entrypoint_provenance"))
    repository_root = _absolute_path(evidence.get("repository_root"))
    if (
        process_id <= 0
        or parent_process_id < 0
        or not creation_date
        or creation_time_utc_ticks <= 0
        or not executable_path
        or ntpath.basename(executable_path).lower() != "python.exe"
        or invocation_mode != "python_script"
        or process_entrypoint != "main.py"
        or not resolved_entrypoint_path
        or not repository_root
        or resolved_entrypoint_path != ntpath.join(repository_root, "main.py")
        or provenance != "exact_repository_script"
    ):
        return None
    ancestor = _canonical_ancestor(evidence.get("approved_ancestor"), repository_root)
    if evidence.get("approved_ancestor") is not None and ancestor is None:
        return None
    venv_redirect = _canonical_venv_redirect(
        evidence.get("venv_redirect"),
        repository_root,
        parent_process_id,
    )
    if evidence.get("venv_redirect") is not None and venv_redirect is None:
        return None
    if (
        ancestor is not None
        and int(ancestor["creation_time_utc_ticks"]) > creation_time_utc_ticks
    ):
        return None
    if (
        venv_redirect is not None
        and int(venv_redirect["creation_time_utc_ticks"])
        > creation_time_utc_ticks
    ):
        return None
    return {
        "approved_ancestor": ancestor,
        "entrypoint_provenance": provenance,
        "intended_lavi_creation_date": creation_date,
        "intended_lavi_creation_time_utc_ticks": creation_time_utc_ticks,
        "intended_lavi_executable_path": executable_path,
        "intended_lavi_parent_process_id": parent_process_id,
        "intended_lavi_pid": process_id,
        "process_entrypoint": process_entrypoint,
        "process_invocation_mode": invocation_mode,
        "repository_root": repository_root,
        "resolved_entrypoint_path": resolved_entrypoint_path,
        "venv_redirect": venv_redirect,
    }


def _canonical_venv_redirect(
    value: object,
    repository_root: str,
    expected_process_id: int,
) -> dict[str, object] | None:
    if value is None:
        return None
    if not isinstance(value, Mapping):
        return None
    process_id = _positive_int(value.get("process_id"))
    parent_process_id = _nonnegative_int(value.get("parent_process_id"))
    creation_date = _required_text(value.get("creation_date"))
    creation_time_utc_ticks = _positive_int(value.get("creation_time_utc_ticks"))
    executable_path = _absolute_path(value.get("executable_path"))
    resolved_entrypoint_path = _absolute_path(
        value.get("resolved_entrypoint_path")
    )
    provenance = _required_text(value.get("entrypoint_provenance"))
    if (
        process_id != expected_process_id
        or parent_process_id < 0
        or not creation_date
        or creation_time_utc_ticks <= 0
        or executable_path
        != normalize_windows_path(
            ntpath.join(repository_root, "venv", "Scripts", "python.exe")
        )
        or resolved_entrypoint_path
        != normalize_windows_path(ntpath.join(repository_root, "main.py"))
        or provenance != "exact_repository_venv_redirect"
    ):
        return None
    return {
        "creation_date": creation_date,
        "creation_time_utc_ticks": creation_time_utc_ticks,
        "entrypoint_provenance": provenance,
        "executable_path": executable_path,
        "parent_process_id": parent_process_id,
        "process_id": process_id,
        "resolved_entrypoint_path": resolved_entrypoint_path,
    }


def _canonical_ancestor(
    value: object,
    repository_root: str,
) -> dict[str, object] | None:
    if value is None:
        return None
    if not isinstance(value, Mapping):
        return None
    process_id = _positive_int(value.get("process_id"))
    parent_process_id = _nonnegative_int(value.get("parent_process_id"))
    creation_date = _required_text(value.get("creation_date"))
    creation_time_utc_ticks = _positive_int(value.get("creation_time_utc_ticks"))
    executable_path = _absolute_path(value.get("executable_path"))
    invocation_mode = _required_text(value.get("invocation_mode"))
    resolved_entrypoint_path = _absolute_path(value.get("resolved_entrypoint_path"))
    provenance = _required_text(value.get("entrypoint_provenance"))
    if (
        process_id <= 0
        or parent_process_id < 0
        or not creation_date
        or creation_time_utc_ticks <= 0
        or not executable_path
        or not is_approved_command_processor_path(executable_path)
        or invocation_mode != "cmd_launcher"
        or not resolved_entrypoint_path
        or resolved_entrypoint_path
        not in {
            ntpath.join(repository_root, "run.bat"),
            ntpath.join(repository_root, "run_lav_dev.cmd"),
        }
        or provenance != "exact_repository_launcher"
    ):
        return None
    return {
        "creation_date": creation_date,
        "creation_time_utc_ticks": creation_time_utc_ticks,
        "entrypoint_provenance": provenance,
        "executable_path": executable_path,
        "invocation_mode": invocation_mode,
        "parent_process_id": parent_process_id,
        "process_id": process_id,
        "resolved_entrypoint_path": resolved_entrypoint_path,
    }


def _positive_int(value: object) -> int:
    return value if type(value) is int and value > 0 else -1


def _nonnegative_int(value: object) -> int:
    return value if type(value) is int and value >= 0 else -1


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _absolute_path(value: object) -> str:
    normalized = normalize_windows_path(value)
    return normalized if normalized and ntpath.isabs(normalized) else ""
