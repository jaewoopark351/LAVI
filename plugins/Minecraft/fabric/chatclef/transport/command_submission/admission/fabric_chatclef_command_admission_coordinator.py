#20260905_kpopmodder: Coordinate read-only admission and one atomic ownership commit.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from .fabric_chatclef_command_admission_decision import (
    FabricChatClefCommandAdmissionDecision,
)


class FabricChatClefCommandAdmissionCoordinator:
    def __init__(
        self,
        *,
        command_lock,
        request_normalizer,
        inspector,
        ownership_committer,
        decision_factory,
        now_ms,
    ) -> None:
        self._command_lock = command_lock
        self._request_normalizer = request_normalizer
        self._inspector = inspector
        self._ownership_committer = ownership_committer
        self._decision_factory = decision_factory
        self._now_ms = now_ms

    def inspect(
        self,
        request: CommandRequestDTO,
        *,
        message_id: str,
        loop: Any,
    ) -> FabricChatClefCommandAdmissionDecision:
        command_request = self._request_normalizer(request)
        with self._command_lock:
            inspection = self._inspector.inspect(command_request, loop=loop)
            if not inspection.allowed:
                return self._decision_factory.rejected(inspection)
            context = self._ownership_committer.begin_locked(
                request=inspection.request,
                message_id=message_id,
            )
            if context is None:
                return self._decision_factory.begin_failed(
                    inspection.request,
                    commands=self._ownership_committer.snapshot_locked(),
                )
            return self._decision_factory.accepted(
                inspection.request,
                command_context=context,
                accepted_at_ms=self._now_ms(),
                commands=self._ownership_committer.snapshot_locked(),
            )
