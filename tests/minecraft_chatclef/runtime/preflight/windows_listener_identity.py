#20260818_kpopmodder: Validate targeted Windows listener evidence without probing processes.
from __future__ import annotations

import re
from pathlib import Path
from typing import Mapping


def validate_listener_probe_payload(
    payload: Mapping[str, object],
    *,
    gradio_port: int,
    fabric_port: int,
    gradio_range_start: int,
    gradio_range_end: int,
    repository_root: str,
) -> dict[str, object]:
    listeners = payload.get("listeners")
    processes = payload.get("processes")
    if not isinstance(listeners, list) or not isinstance(processes, list):
        return _failure("listener probe fields are missing")
    owners: dict[int, set[int]] = {}
    for item in listeners:
        if not isinstance(item, Mapping):
            continue
        port = _int_value(item.get("local_port"))
        process_id = _int_value(item.get("process_id"))
        if port > 0 and process_id > 0:
            owners.setdefault(port, set()).add(process_id)
    gradio_owners = owners.get(gradio_port, set())
    fabric_owners = owners.get(fabric_port, set())
    if len(gradio_owners) != 1:
        return _failure("selected Gradio endpoint must have one listener owner")
    if len(fabric_owners) != 1:
        return _failure("Fabric endpoint must have one listener owner")
    gradio_pid = next(iter(gradio_owners))
    fabric_pid = next(iter(fabric_owners))
    if gradio_pid != fabric_pid:
        return _failure("Gradio and Fabric listeners have different owners")
    process_map = _process_map(processes)
    entrypoint = _approved_entrypoint(gradio_pid, process_map, repository_root)
    if not entrypoint:
        return _failure("listener owner is not an approved LAVI entrypoint")
    owner = process_map.get(gradio_pid, {})
    creation_date = str(owner.get("creation_date") or "").strip()
    if not creation_date:
        return _failure("listener owner creation date is unavailable")
    for port in range(gradio_range_start, gradio_range_end + 1):
        if port == gradio_port:
            continue
        for process_id in owners.get(port, set()):
            if process_id == gradio_pid:
                continue
            if _approved_entrypoint(process_id, process_map, repository_root):
                return _failure("a second LAVI Gradio candidate is listening")
    return {
        "ok": True,
        "reason": "listener_process_identity_validated",
        "observed": {
            "listener_pid_by_port": {
                str(gradio_port): gradio_pid,
                str(fabric_port): fabric_pid,
            },
            "process_entrypoint": entrypoint,
            "intended_lavi_pid": gradio_pid,
            "intended_lavi_creation_date": creation_date,
        },
    }


def _process_map(processes: list[object]) -> dict[int, dict[str, object]]:
    output: dict[int, dict[str, object]] = {}
    for item in processes:
        if not isinstance(item, Mapping):
            continue
        process_id = _int_value(item.get("process_id"))
        if process_id > 0:
            output[process_id] = dict(item)
    return output


def _approved_entrypoint(
    process_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> str:
    root = _normalized_path(repository_root)
    current = process_id
    seen: set[int] = set()
    evidence: list[str] = []
    for _depth in range(10):
        if current <= 0 or current in seen:
            break
        seen.add(current)
        process = processes.get(current)
        if process is None:
            break
        evidence.extend(
            [
                str(process.get("command_line") or ""),
                str(process.get("executable_path") or ""),
                str(process.get("name") or ""),
            ]
        )
        current = _int_value(process.get("parent_process_id"))
    combined = " ".join(evidence).lower().replace("/", "\\")
    if root not in combined:
        return ""
    patterns = (
        (r"(?:^|[\\\s])main\.py(?:[\"']|\s|$)", "main.py"),
        (r"(?:^|\s)-m\s+lavi(?:\s+app)?(?:\s|$)", "python -m lavi"),
        (r"run\.bat(?:\s|$)", "run.bat"),
        (r"run_lav_dev\.cmd(?:\s|$)", "run_lav_dev.cmd"),
    )
    for pattern, label in patterns:
        if re.search(pattern, combined):
            return label
    return ""


def _normalized_path(path: str) -> str:
    return str(Path(path)).lower().replace("/", "\\").rstrip("\\")


def _int_value(value: object) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return -1


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": {}}
