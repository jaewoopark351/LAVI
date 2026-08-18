#20260819_kpopmodder: Keep command-admission result data separate from admission policy.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode


@dataclass(frozen=True)
class FabricChatClefCommandAdmissionDecision:
    request: CommandRequestDTO
    command_context: Any = None
    error_code: BridgeErrorCode | None = None
    message: str = ""
    event: str = ""
    details: dict[str, Any] | None = None

    @property
    def accepted(self) -> bool:
        return self.command_context is not None and self.error_code is None
