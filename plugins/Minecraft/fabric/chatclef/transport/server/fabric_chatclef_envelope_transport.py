#20260818_kpopmodder: Own Fabric ChatClef bridge-envelope serialization and replies.
from __future__ import annotations

import json
from typing import Any, Callable

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefEnvelopeTransport:
    def __init__(
        self,
        *,
        message_id_factory: Callable[[], str],
        now_ms: Callable[[], int],
    ):
        self._message_id_factory = message_id_factory
        self._now_ms = now_ms

    def parse(self, raw_message: Any) -> BridgeEnvelopeDTO | None:
        try:
            payload = json.loads(raw_message)
            return BridgeEnvelopeDTO.from_mapping(payload)
        except Exception:
            return None

    async def send(self, websocket: Any, envelope: BridgeEnvelopeDTO) -> None:
        await websocket.send(
            json.dumps(
                envelope.to_dict(),
                ensure_ascii=False,
                separators=(",", ":"),
            )
        )

    async def send_handshake_ack(
        self,
        websocket: Any,
        request: BridgeEnvelopeDTO,
        session_id: str,
        status: dict[str, Any],
        *,
        accepted: bool = True,
        message: str = "",
        connection_generation: int = 0,
    ) -> None:
        await self.send(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.HANDSHAKE_ACK,
                message_id=self._message_id_factory(),
                correlation_id=request.message_id,
                session_id=session_id,
                timestamp_ms=self._now_ms(),
                payload={
                    "accepted": accepted,
                    "session_id": session_id,
                    "connection_generation": connection_generation,
                    "message": message,
                    "status": status,
                },
            ),
        )

    async def send_status_snapshot(
        self,
        websocket: Any,
        request: BridgeEnvelopeDTO,
        status: dict[str, Any],
    ) -> None:
        await self.send(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.STATUS_SNAPSHOT,
                message_id=self._message_id_factory(),
                correlation_id=request.message_id,
                session_id=request.session_id,
                timestamp_ms=self._now_ms(),
                payload=status,
            ),
        )

    async def send_error(
        self,
        websocket: Any,
        *,
        error_code: BridgeErrorCode,
        message: str,
        request: BridgeEnvelopeDTO | None = None,
    ) -> None:
        await self.send(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.ERROR,
                message_id=self._message_id_factory(),
                correlation_id=None if request is None else request.message_id,
                session_id=None if request is None else request.session_id,
                timestamp_ms=self._now_ms(),
                payload={
                    "error_code": error_code.value,
                    "message": message,
                },
            ),
        )
