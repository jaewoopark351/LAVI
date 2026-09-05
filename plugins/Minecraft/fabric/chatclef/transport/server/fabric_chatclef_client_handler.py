#20260905_kpopmodder: Preserve the client-handler API as one session-loop delegation facade.
from __future__ import annotations

from typing import Any, Callable

from .client_session import (
    FabricChatClefClientSessionComponentGraph,
)


class FabricChatClefClientHandler:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
        envelope_transport,
        command_result_handler,
        stop_control_result_demultiplexer=None,
        status_provider: Callable[[], dict[str, Any]],
        stopping_provider: Callable[[], bool],
        last_error_reporter: Callable[[str], None],
        now_ms: Callable[[], int],
    ) -> None:
        self._components = FabricChatClefClientSessionComponentGraph(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
            envelope_transport=envelope_transport,
            command_result_handler=command_result_handler,
            stop_control_result_demultiplexer=stop_control_result_demultiplexer,
            status_provider=status_provider,
            stopping_provider=stopping_provider,
            last_error_reporter=last_error_reporter,
            now_ms=now_ms,
        )
        self._envelope_reader = self._components.envelope_reader
        self._handshake_handler = self._components.handshake_handler
        self._active_message_dispatcher = self._components.active_message_dispatcher
        self._disconnect_cleanup = self._components.disconnect_cleanup

    async def handle_legacy_client(self, websocket: Any, _path: str = "") -> None:
        await self.handle_client(websocket)

    async def handle_client(self, websocket: Any) -> None:
        await self._components.coordinator.handle(websocket)
