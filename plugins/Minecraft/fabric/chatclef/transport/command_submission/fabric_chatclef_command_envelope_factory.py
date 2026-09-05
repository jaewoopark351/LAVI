#20260905_kpopmodder: Create only Fabric ChatClef ordinary-command identifiers and envelopes.
from __future__ import annotations

import time
import uuid

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefCommandEnvelopeFactory:
    def __init__(self, *, message_id_factory, now_ms) -> None:
        self._message_id_factory = message_id_factory
        self._now_ms = now_ms

    def new_message_id(self) -> str:
        return self._message_id_factory()

    def build(self, *, request, command_context, message_id: str) -> BridgeEnvelopeDTO:
        return BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_REQUEST,
            message_id=message_id,
            correlation_id=request.request_id,
            session_id=command_context.session_id,
            timestamp_ms=self._now_ms(),
            payload=request.to_dict(),
        )


def default_fabric_chatclef_message_id() -> str:
    return f"lavi-{uuid.uuid4().hex}"


def system_now_ms() -> int:
    return int(time.time() * 1000)
