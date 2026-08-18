#20260818_kpopmodder: Orchestrate one admitted Fabric ChatClef command delivery.
from __future__ import annotations

import asyncio
import uuid
from typing import Any, Callable

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType

from .fabric_chatclef_command_admission import FabricChatClefCommandAdmission
from .fabric_chatclef_command_delivery import FabricChatClefCommandDelivery
from .fabric_chatclef_command_result_factory import (
    FabricChatClefCommandResultFactory,
)
from .fabric_chatclef_command_submission_diagnostics import (
    FabricChatClefCommandSubmissionDiagnostics,
)


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
    ):
        self._loop_provider = loop_provider
        self._envelope_transport = envelope_transport
        self._message_id_factory = message_id_factory or (
            lambda: f"lavi-{uuid.uuid4().hex}"
        )
        self._now_ms = now_ms or _system_now_ms
        self._admission = FabricChatClefCommandAdmission(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            now_ms=self._now_ms,
        )
        self._delivery = FabricChatClefCommandDelivery(
            future_scheduler=future_scheduler,
            send_timeout_sec=send_timeout_sec,
        )
        self._results = FabricChatClefCommandResultFactory()
        self._events = FabricChatClefCommandSubmissionDiagnostics(diagnostics)

    def submit(self, request: CommandRequestDTO) -> CommandResultDTO:
        loop = self._loop_provider()
        message_id = self._message_id_factory()
        decision = self._admission.inspect(
            request,
            message_id=message_id,
            loop=loop,
        )
        self._events.log(decision.event, decision.request, decision.details or {})
        if not decision.accepted:
            return self._results.rejected(
                decision.request,
                decision.error_code or BridgeErrorCode.INTERNAL_ERROR,
                decision.message,
            )

        context = decision.command_context
        envelope = BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_REQUEST,
            message_id=message_id,
            correlation_id=decision.request.request_id,
            session_id=context.session_id,
            timestamp_ms=self._now_ms(),
            payload=decision.request.to_dict(),
        )
        delivery = self._delivery.deliver(
            self._envelope_transport.send(context.websocket, envelope),
            loop,
        )
        if delivery.status == "not_scheduled":
            commands = self._admission.release_if_not_scheduled(context)
            self._events.log(
                "command_schedule_failed",
                decision.request,
                _error_details(delivery.error, commands),
            )
            return self._results.rejected(
                decision.request,
                BridgeErrorCode.INTERNAL_ERROR,
                "Fabric ChatClef command could not be scheduled: "
                f"{_error_text(delivery.error)}",
            )
        if delivery.status == "outcome_unknown":
            commands = self._admission.snapshot()
            details = _error_details(delivery.error, commands)
            details["command_ownership_retained"] = True
            self._events.log(
                "command_send_outcome_unknown",
                decision.request,
                details,
            )
            return self._results.unknown(
                decision.request,
                context,
                delivery.error or RuntimeError("unknown delivery error"),
            )

        self._events.log(
            "command_send_succeeded",
            decision.request,
            {
                "session_id": context.session_id,
                "connection_generation": context.generation,
                "command_message_id": context.command_message_id,
                "sent_at_ms": self._now_ms(),
                "commands": self._admission.snapshot(),
            },
        )
        return self._results.accepted(decision.request, context)


def _error_details(error: Exception | None, commands) -> dict[str, Any]:
    return {
        "error_type": type(error).__name__ if error is not None else "UnknownError",
        "error_message": str(error or "unknown error"),
        "commands": commands,
    }


def _error_text(error: Exception | None) -> str:
    if error is None:
        return "UnknownError: unknown error"
    return f"{type(error).__name__}: {error}"


def _system_now_ms() -> int:
    import time

    return int(time.time() * 1000)
