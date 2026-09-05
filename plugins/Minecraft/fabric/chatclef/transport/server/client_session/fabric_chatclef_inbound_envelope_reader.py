#20260905_kpopmodder: Parse and validate one inbound Fabric ChatClef protocol envelope.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode


class FabricChatClefInboundEnvelopeReader:
    def __init__(self, *, envelope_transport) -> None:
        self._envelope_transport = envelope_transport

    async def read(self, websocket: Any, raw_message: Any):
        envelope = self._envelope_transport.parse(raw_message)
        if envelope is None:
            await self._envelope_transport.send_error(
                websocket,
                error_code=BridgeErrorCode.INVALID_REQUEST,
                message="Invalid Fabric ChatClef bridge JSON envelope.",
            )
            return None
        if envelope.protocol_version != 1:
            await self._envelope_transport.send_error(
                websocket,
                request=envelope,
                error_code=BridgeErrorCode.UNSUPPORTED_PROTOCOL_VERSION,
                message="Only Fabric ChatClef bridge protocol v1 is supported.",
            )
            return None
        return envelope
