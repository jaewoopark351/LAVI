#20260905_kpopmodder: Preserve ordinary-command submission as a sequencing facade.
from __future__ import annotations

import asyncio
from typing import Any, Callable

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO

from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_submission_component_graph import FabricChatClefCommandSubmissionComponentGraph


class FabricChatClefCommandSubmitter:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock: Any,
        diagnostics: Any,
        loop_provider: Callable[[], Any],
        envelope_transport: Any,
        send_timeout_sec: float,
        future_scheduler: Callable[[Any, Any], Any] = (
            asyncio.run_coroutine_threadsafe
        ),
        message_id_factory: Callable[[], str] | None = None,
        now_ms: Callable[[], int] | None = None,
        stop_control_admission_barrier: object = None,
    ) -> None:
        self._components = FabricChatClefCommandSubmissionComponentGraph(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            diagnostics=diagnostics,
            loop_provider=loop_provider,
            envelope_transport=envelope_transport,
            send_timeout_sec=send_timeout_sec,
            future_scheduler=future_scheduler,
            message_id_factory=message_id_factory,
            now_ms=now_ms,
            stop_control_admission_barrier=stop_control_admission_barrier,
        )
        self._loop_provider = self._components.loop_provider
        self._envelope_transport = self._components.envelope_transport
        self._message_id_factory = self._components.message_id_factory
        self._now_ms = self._components.now_ms
        self._admission = self._components.admission
        self._delivery = self._components.delivery
        self._results = self._components.results
        self._events = self._components.events

    def submit(self, request: CommandRequestDTO) -> CommandResultDTO:
        return self._components.sequence.submit(request)
