#20260818_kpopmodder: Validate exact loopback listeners and their LAVI process owner.
from __future__ import annotations

from typing import Mapping

from .process_entrypoint_identity import approved_entrypoint


LOOPBACK_ADDRESSES = {"127.0.0.1", "::1"}


def validate_listener_probe_payload(
    payload: Mapping[str, object],
    *,
    gradio_port: int,
    fabric_port: int,
    gradio_range_start: int,
    gradio_range_end: int,
    repository_root: str,
    gradio_host: str,
    fabric_host: str,
) -> dict[str, object]:
    listeners = payload.get("listeners")
    processes = payload.get("processes")
    if not isinstance(listeners, list) or not isinstance(processes, list):
        return _failure("listener probe fields are missing")
    entries = _listener_entries(listeners)
    gradio_pid, gradio_error = _target_owner(
        entries,
        gradio_port,
        gradio_host,
        "selected Gradio endpoint",
    )
    if gradio_error:
        return _failure(gradio_error)
    fabric_pid, fabric_error = _target_owner(
        entries,
        fabric_port,
        fabric_host,
        "Fabric endpoint",
    )
    if fabric_error:
        return _failure(fabric_error)
    if gradio_pid != fabric_pid:
        return _failure("Gradio and Fabric listeners have different owners")

    process_map = _process_map(processes)
    entrypoint = approved_entrypoint(gradio_pid, process_map, repository_root)
    if not entrypoint:
        return _failure("listener owner is not an approved LAVI entrypoint")
    owner = process_map.get(gradio_pid, {})
    creation_date = str(owner.get("creation_date") or "").strip()
    if not creation_date:
        return _failure("listener owner creation date is unavailable")
    for port in range(gradio_range_start, gradio_range_end + 1):
        if port == gradio_port:
            continue
        for process_id in _owners_for_port(entries, port):
            if process_id == gradio_pid:
                continue
            if approved_entrypoint(process_id, process_map, repository_root):
                return _failure("a second LAVI Gradio candidate is listening")
    return {
        "ok": True,
        "reason": "listener_process_identity_validated",
        "observed": {
            "listener_pid_by_port": {
                str(gradio_port): gradio_pid,
                str(fabric_port): fabric_pid,
            },
            "listener_address_by_port": {
                str(gradio_port): gradio_host,
                str(fabric_port): fabric_host,
            },
            "process_entrypoint": entrypoint,
            "intended_lavi_pid": gradio_pid,
            "intended_lavi_creation_date": creation_date,
        },
    }


def _listener_entries(listeners: list[object]) -> list[tuple[int, int, str]]:
    entries: list[tuple[int, int, str]] = []
    for item in listeners:
        if not isinstance(item, Mapping):
            continue
        port = _int_value(item.get("local_port"))
        process_id = _int_value(item.get("process_id"))
        address = str(item.get("local_address") or "").strip().lower()
        if port > 0 and process_id > 0:
            entries.append((port, process_id, address))
    return entries


def _target_owner(
    entries: list[tuple[int, int, str]],
    port: int,
    expected_host: str,
    label: str,
) -> tuple[int, str]:
    expected = str(expected_host or "").strip().lower()
    if expected not in LOOPBACK_ADDRESSES:
        return -1, f"{label} expected host is not exact loopback"
    selected = [entry for entry in entries if entry[0] == port]
    owners = {entry[1] for entry in selected}
    if len(owners) != 1:
        return -1, f"{label} must have one listener owner"
    addresses = {entry[2] for entry in selected}
    if not addresses or any(address not in LOOPBACK_ADDRESSES for address in addresses):
        return -1, f"{label} listener must bind only exact loopback"
    if addresses != {expected}:
        return -1, f"{label} listener address does not match the approved URL family"
    return next(iter(owners)), ""


def _owners_for_port(
    entries: list[tuple[int, int, str]],
    port: int,
) -> set[int]:
    return {process_id for entry_port, process_id, _address in entries if entry_port == port}


def _process_map(processes: list[object]) -> dict[int, dict[str, object]]:
    output: dict[int, dict[str, object]] = {}
    for item in processes:
        if not isinstance(item, Mapping):
            continue
        process_id = _int_value(item.get("process_id"))
        if process_id > 0:
            output[process_id] = dict(item)
    return output


def _int_value(value: object) -> int:
    if isinstance(value, bool):
        return -1
    try:
        return int(value)
    except (TypeError, ValueError):
        return -1


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": {}}
