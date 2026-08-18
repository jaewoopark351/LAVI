#20260818_kpopmodder: Compose exact listener topology and structured LAVI process identity.
#20260819_kpopmodder: Compose split topology, probe evidence, and process provenance checks.
from __future__ import annotations

from typing import Mapping

from .listener_probe_evidence import listener_entries, process_map
from .listener_topology_identity import owners_for_port, target_owner
from .process_identity.process_ancestry import inspect_approved_process_identity


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
    entries, entries_error = listener_entries(listeners)
    if entries_error:
        return _failure(entries_error)
    gradio_pid, gradio_error = target_owner(
        entries,
        gradio_port,
        gradio_host,
        "selected Gradio endpoint",
    )
    if gradio_error:
        return _failure(gradio_error)
    fabric_pid, fabric_error = target_owner(
        entries,
        fabric_port,
        fabric_host,
        "Fabric endpoint",
    )
    if fabric_error:
        return _failure(fabric_error)
    if gradio_pid != fabric_pid:
        return _failure("Gradio and Fabric listeners have different owners")

    processes_by_id, process_error = process_map(processes)
    if process_error:
        return _failure(process_error)
    process_identity = inspect_approved_process_identity(
        gradio_pid,
        processes_by_id,
        repository_root,
    )
    if not process_identity.get("ok"):
        return _failure("listener owner is not an approved LAVI entrypoint")
    for port in range(gradio_range_start, gradio_range_end + 1):
        if port == gradio_port:
            continue
        for process_id in owners_for_port(entries, port):
            if process_id == gradio_pid:
                continue
            candidate = inspect_approved_process_identity(
                process_id,
                processes_by_id,
                repository_root,
            )
            if candidate.get("ok"):
                return _failure("a second LAVI Gradio candidate is listening")
    process_observed = process_identity.get("observed")
    if not isinstance(process_observed, Mapping):
        return _failure("listener owner process identity evidence is unavailable")
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
            **dict(process_observed),
        },
    }


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "observed": {}}
