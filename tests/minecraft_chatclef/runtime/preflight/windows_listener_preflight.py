#20260818_kpopmodder: Compose listener evidence collection and identity validation.
from __future__ import annotations

from typing import Callable, Mapping

from .windows_listener_identity import validate_listener_probe_payload
from .windows_listener_probe import read_windows_listener_probe


def inspect_windows_listener_identity(
    *,
    gradio_port: int,
    fabric_port: int,
    gradio_range_start: int,
    gradio_range_end: int,
    repository_root: str,
    probe_reader: Callable[..., dict[str, object]] = read_windows_listener_probe,
) -> dict[str, object]:
    ports = list(range(gradio_range_start, gradio_range_end + 1))
    ports.extend((gradio_port, fabric_port))
    probe = probe_reader(ports)
    if not probe.get("ok"):
        return {
            "ok": False,
            "reason": str(probe.get("reason") or "listener probe failed"),
            "observed": {},
        }
    payload = probe.get("payload")
    if not isinstance(payload, Mapping):
        return {"ok": False, "reason": "listener probe payload is missing", "observed": {}}
    return validate_listener_probe_payload(
        payload,
        gradio_port=gradio_port,
        fabric_port=fabric_port,
        gradio_range_start=gradio_range_start,
        gradio_range_end=gradio_range_end,
        repository_root=repository_root,
    )
