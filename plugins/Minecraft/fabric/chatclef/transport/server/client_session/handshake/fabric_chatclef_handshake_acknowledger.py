#20260905_kpopmodder: Isolate Fabric ChatClef handshake acknowledgement delivery.
from __future__ import annotations

from typing import Any, Callable


class FabricChatClefHandshakeAcknowledger:
    def __init__(
        self,
        *,
        envelope_transport,
        status_provider: Callable[[], dict[str, Any]],
    ) -> None:
        self._envelope_transport = envelope_transport
        self._status_provider = status_provider

    async def send(
        self,
        websocket: Any,
        envelope: object,
        session_id: str,
        *,
        accepted: bool,
        connection_generation: int,
        message: str | None = None,
    ) -> None:
        kwargs = {
            "accepted": accepted,
            "connection_generation": connection_generation,
        }
        if message is not None:
            kwargs["message"] = message
        await self._envelope_transport.send_handshake_ack(
            websocket,
            envelope,
            session_id,
            self._status_provider(),
            **kwargs,
        )


__all__ = ("FabricChatClefHandshakeAcknowledger",)
