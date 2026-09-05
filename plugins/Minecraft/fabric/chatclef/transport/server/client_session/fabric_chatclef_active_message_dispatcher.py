#20260905_kpopmodder: Dispatch messages only after active Fabric ChatClef session proof.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefActiveMessageDispatcher:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        diagnostics,
        envelope_transport,
        command_result_handler,
        stop_control_result_demultiplexer,
        status_provider: Callable[[], dict[str, Any]],
    ) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._diagnostics = diagnostics
        self._envelope_transport = envelope_transport
        self._command_result_handler = command_result_handler
        self._stop_control_result_demultiplexer = stop_control_result_demultiplexer
        self._status_provider = status_provider

    async def dispatch(self, websocket: Any, envelope) -> None:
        with self._command_lock:
            is_active_websocket = self._connection_ownership.is_active_websocket(
                websocket
            )
        if not is_active_websocket:
            await self._envelope_transport.send_error(
                websocket,
                request=envelope,
                error_code=BridgeErrorCode.NOT_CONNECTED,
                message=(
                    "Fabric ChatClef bridge messages require an active "
                    "handshake on this websocket."
                ),
            )
            self._diagnostics.warning(
                "ignored message from inactive websocket "
                f"type={envelope.message_type.value} "
                f"session={envelope.session_id}"
            )
            return

        if envelope.message_type == BridgeMessageType.STATUS_REQUEST:
            await self._envelope_transport.send_status_snapshot(
                websocket,
                envelope,
                self._status_provider(),
            )
            return

        if envelope.message_type == BridgeMessageType.COMMAND_RESULT:
            if (
                self._stop_control_result_demultiplexer is not None
                and self._stop_control_result_demultiplexer.handle(websocket, envelope)
            ):
                return
            self._command_result_handler.handle(websocket, envelope)
            return

        await self._envelope_transport.send_error(
            websocket,
            request=envelope,
            error_code=BridgeErrorCode.NOT_IMPLEMENTED,
            message="Fabric ChatClef bridge message type is not handled.",
        )
