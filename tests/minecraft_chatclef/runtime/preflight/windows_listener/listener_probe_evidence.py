#20260819_kpopmodder: Normalize untrusted listener probe collections without silently coercing identity fields.
from __future__ import annotations

from typing import Mapping


def listener_entries(
    listeners: list[object],
) -> tuple[list[tuple[int, int, str]], str]:
    entries: list[tuple[int, int, str]] = []
    for item in listeners:
        if not isinstance(item, Mapping):
            return [], "listener probe contains a malformed listener"
        port = item.get("local_port")
        process_id = item.get("process_id")
        address = item.get("local_address")
        if (
            type(port) is not int
            or port <= 0
            or type(process_id) is not int
            or process_id <= 0
            or type(address) is not str
            or not address.strip()
        ):
            return [], "listener probe contains incomplete listener identity"
        entries.append((port, process_id, address.strip().lower()))
    return entries, ""


def process_map(
    processes: list[object],
) -> tuple[dict[int, dict[str, object]], str]:
    output: dict[int, dict[str, object]] = {}
    for item in processes:
        if not isinstance(item, Mapping):
            return {}, "listener probe contains a malformed process"
        process_id = item.get("process_id")
        if type(process_id) is not int or process_id <= 0:
            return {}, "listener probe contains incomplete process identity"
        if process_id in output:
            return {}, "listener probe contains duplicate process identity"
        output[process_id] = dict(item)
    return output, ""
