#20260905_kpopmodder: Preserve ordinary-command admission as a focused compatibility facade.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO

from .admission import FabricChatClefCommandAdmissionComponentGraph
from .fabric_chatclef_command_admission_decision import (
    FabricChatClefCommandAdmissionDecision,
)


class FabricChatClefCommandAdmission:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        now_ms,
        stop_control_admission_barrier=None,
    ) -> None:
        self._components = FabricChatClefCommandAdmissionComponentGraph(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            now_ms=now_ms,
            stop_control_admission_barrier=stop_control_admission_barrier,
        )

    def inspect(
        self,
        request: CommandRequestDTO,
        *,
        message_id: str,
        loop: Any,
    ) -> FabricChatClefCommandAdmissionDecision:
        return self._components.coordinator.inspect(
            request,
            message_id=message_id,
            loop=loop,
        )

    def release_if_not_scheduled(self, command_context: Any) -> dict[str, Any]:
        return self._components.ownership_committer.release_if_not_scheduled(
            command_context
        )

    def snapshot(self) -> dict[str, Any]:
        return self._components.ownership_committer.snapshot()
