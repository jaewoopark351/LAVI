#20260905_kpopmodder: Carry one immutable read-only ordinary-command admission inspection.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode


@dataclass(frozen=True)
class FabricChatClefOrdinaryAdmissionInspection:
    request: CommandRequestDTO
    allowed: bool
    error_code: BridgeErrorCode | None = None
    message: str = ""
    event: str = ""
    details: Mapping[str, Any] | None = None
