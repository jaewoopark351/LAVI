#20260905_kpopmodder: Preserve the legacy STOP submitter as a thin facade.
from __future__ import annotations

import asyncio
from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry

from .stop_control_request_factory import StopControlRequestFactory
from .stop_control_submission_result import StopControlSubmissionResult
from .stop_control_transition_logger import StopControlTransitionLogger
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_component_graph import (
    StopControlSubmissionComponentGraph,
)


class StopControlSubmitter:
    def __init__(
        self,
        *,
        connection_ownership: object,
        command_lock: object,
        session_registry: object,
        claim_registry: StopControlClaimRegistry,
        admission_barrier: object,
        tracker_registry: object,
        diagnostics: object,
        loop_provider: Callable[[], Any],
        envelope_transport: object,
        send_timeout_sec: float,
        message_id_factory: Callable[[], str] | None = None,
        request_id_factory: Callable[[], str] | None = None,
        now_ms: Callable[[], int] | None = None,
        future_scheduler: Callable[
            [Any, Any], Any
        ] = asyncio.run_coroutine_threadsafe,
        request_factory: StopControlRequestFactory | None = None,
        transition_logger: StopControlTransitionLogger | None = None,
    ):
        self._component_graph = StopControlSubmissionComponentGraph(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            session_registry=session_registry,
            claim_registry=claim_registry,
            admission_barrier=admission_barrier,
            tracker_registry=tracker_registry,
            diagnostics=diagnostics,
            loop_provider=loop_provider,
            envelope_transport=envelope_transport,
            send_timeout_sec=send_timeout_sec,
            message_id_factory=message_id_factory,
            request_id_factory=request_id_factory,
            now_ms=now_ms,
            future_scheduler=future_scheduler,
            request_factory=request_factory,
            transition_logger=transition_logger,
        )
        self._claim_registry = self._component_graph.claim_registry
        self._coordinator = self._component_graph.coordinator

    def submit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ) -> StopControlSubmissionResult:
        return self._coordinator.submit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )


__all__ = ("StopControlSubmitter",)
