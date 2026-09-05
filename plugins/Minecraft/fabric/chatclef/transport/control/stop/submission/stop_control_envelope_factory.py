#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import time
import uuid

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_identity import (
    StopControlIdentity,
)


class StopControlEnvelopeFactory:
    def __init__(
        self,
        *,
        request_factory: object,
        message_id_factory=None,
        request_id_factory=None,
        now_ms=None,
    ):
        self._request_factory = request_factory
        self._message_id_factory = message_id_factory or (
            lambda: f"lavi-{uuid.uuid4().hex}"
        )
        self._request_id_factory = request_id_factory or (
            lambda: f"lavi-stop-{uuid.uuid4().hex}"
        )
        self._now_ms = now_ms or self._system_now_ms

    def create(
        self,
        *,
        event: object,
        target: object,
        session_id: object,
        generation: object,
    ) -> tuple[object, object, BridgeEnvelopeDTO]:
        identity = StopControlIdentity(
            request_id=self._request_id_factory(),
            message_id=self._message_id_factory(),
            session_id=str(session_id),
            server_connection_generation=generation,
        )
        request = self._request_factory.build(
            identity=identity,
            event=event,
            now_ms=self._now_ms(),
            target=target,
        )
        envelope = BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_REQUEST,
            message_id=identity.message_id,
            correlation_id=identity.request_id,
            session_id=identity.session_id,
            timestamp_ms=self._now_ms(),
            payload=request.to_dict(),
        )
        return identity, request, envelope

    def _system_now_ms(self) -> int:
        return int(time.time() * 1000)


__all__ = ("StopControlEnvelopeFactory",)
