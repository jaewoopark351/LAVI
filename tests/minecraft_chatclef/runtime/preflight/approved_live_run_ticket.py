#20260819_kpopmodder: Bind one approved live command and runtime identity into an immutable ticket.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ApprovedLiveRunTicket:
    command: str
    command_fingerprint: str
    gradio_url: str
    expected_backend: str
    expected_instance: str
    expected_world: str
    invocation_id: str
    approval_json: str
    repository_root: str
    process_identity_fingerprint: str
