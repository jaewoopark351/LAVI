#20260905_kpopmodder: Freeze an ordinary command identity without taking over its owner slot.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class StopControlTargetSnapshot:
    request_id: str
    command_message_id: str
    session_id: str
    server_connection_generation: int
    owner_token: object = field(compare=False, repr=False)

    @property
    def complete(self) -> bool:
        return (
            type(self.request_id) is str
            and bool(self.request_id)
            and type(self.command_message_id) is str
            and bool(self.command_message_id)
            and type(self.session_id) is str
            and bool(self.session_id)
            and type(self.server_connection_generation) is int
            and self.server_connection_generation > 0
            and self.owner_token is not None
            and getattr(self.owner_token, "request_id", None) == self.request_id
            and getattr(self.owner_token, "command_message_id", None)
            == self.command_message_id
            and getattr(self.owner_token, "session_id", None) == self.session_id
            and getattr(self.owner_token, "generation", None)
            == self.server_connection_generation
        )


__all__ = ("StopControlTargetSnapshot",)
