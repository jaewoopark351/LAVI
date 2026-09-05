#20260905_kpopmodder: Coordinate one accepted Fabric ChatClef client session loop.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefClientSessionCoordinator:
    def __init__(
        self,
        *,
        envelope_reader,
        handshake_handler,
        active_message_dispatcher,
        disconnect_cleanup,
        diagnostics,
        stopping_provider: Callable[[], bool],
        last_error_reporter: Callable[[str], None],
    ) -> None:
        self._envelope_reader = envelope_reader
        self._handshake_handler = handshake_handler
        self._active_message_dispatcher = active_message_dispatcher
        self._disconnect_cleanup = disconnect_cleanup
        self._diagnostics = diagnostics
        self._stopping_provider = stopping_provider
        self._last_error_reporter = last_error_reporter

    async def handle(self, websocket: Any) -> None:
        accepted_session_id: str | None = None
        connection_generation = 0
        try:
            async for raw_message in websocket:
                envelope = await self._envelope_reader.read(websocket, raw_message)
                if envelope is None:
                    continue
                if envelope.message_type == BridgeMessageType.HANDSHAKE:
                    accepted, session_id, generation = (
                        await self._handshake_handler.handle(websocket, envelope)
                    )
                    if accepted:
                        accepted_session_id = session_id
                        connection_generation = generation
                    continue
                await self._active_message_dispatcher.dispatch(websocket, envelope)
        except Exception as error:
            if not self._stopping_provider():
                message = f"{type(error).__name__}: {error}"
                self._last_error_reporter(message)
                self._diagnostics.warning(f"client handler ended with {message}")
        finally:
            if accepted_session_id is not None:
                self._disconnect_cleanup.release(
                    websocket=websocket,
                    session_id=accepted_session_id,
                    connection_generation=connection_generation,
                )
