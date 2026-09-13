#20260913_kpopmodder: Freeze one request, Task, and original destination binding.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class GotoCommandBinding:
    request_id: str
    command_message_id: str
    session_id: str
    server_connection_generation: int
    java_socket_generation: int
    task_owner: str
    task_identity: str
    operation_id: str
    request_shape: str
    target_x: int
    target_y: int
    target_z: int
    requested_dimension: str | None
    world_dimension: str
