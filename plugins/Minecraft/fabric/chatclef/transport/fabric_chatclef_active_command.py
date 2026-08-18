#20260819_kpopmodder: Keep active-command ownership data in one dedicated type.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class FabricChatClefActiveCommand:
    websocket: Any = field(compare=False, repr=False)
    session_id: str
    generation: int
    request_id: str
    command_message_id: str
    command: str = field(default="", compare=False)
    source: str = field(default="", compare=False)
    started_at_ms: int = field(default=0, compare=False)
