#20260905_kpopmodder: Assemble the STOP submission object graph outside its legacy facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_request_factory import StopControlRequestFactory
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_transition_logger import StopControlTransitionLogger
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_connection_admission_gate import (
    StopControlConnectionAdmissionGate,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_envelope_factory import StopControlEnvelopeFactory
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_prewrite_guard import StopControlPrewriteGuard
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_send_outcome_coordinator import (
    StopControlSendOutcomeCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_admission_coordinator import (
    StopControlSubmissionAdmissionCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_coordinator import (
    StopControlSubmissionCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_result_factory import (
    StopControlSubmissionResultFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_transition_reporter import (
    StopControlSubmissionTransitionReporter,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_target_snapshot_factory import (
    StopControlTargetSnapshotFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_tracker_admission_coordinator import (
    StopControlTrackerAdmissionCoordinator,
)


class StopControlSubmissionComponentGraph:
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
        loop_provider: object,
        envelope_transport: object,
        send_timeout_sec: float,
        message_id_factory: object | None,
        request_id_factory: object | None,
        now_ms: object | None,
        future_scheduler: object,
        request_factory: StopControlRequestFactory | None,
        transition_logger: StopControlTransitionLogger | None,
    ):
        if type(claim_registry) is not StopControlClaimRegistry:
            raise TypeError(
                "STOP submitter requires its authoritative claim registry"
            )
        self.claim_registry = claim_registry

        transitions = transition_logger or StopControlTransitionLogger(
            diagnostics
        )
        result_factory = StopControlSubmissionResultFactory()
        transition_reporter = StopControlSubmissionTransitionReporter(
            transitions
        )
        connection_gate = StopControlConnectionAdmissionGate(
            connection_ownership=connection_ownership,
            session_registry=session_registry,
            tracker_registry=tracker_registry,
            admission_barrier=admission_barrier,
            loop_provider=loop_provider,
        )
        target_snapshot_factory = StopControlTargetSnapshotFactory(
            connection_ownership
        )
        envelope_factory = StopControlEnvelopeFactory(
            request_factory=request_factory or StopControlRequestFactory(),
            message_id_factory=message_id_factory,
            request_id_factory=request_id_factory,
            now_ms=now_ms,
        )
        tracker_admission = StopControlTrackerAdmissionCoordinator(
            admission_barrier=admission_barrier,
            tracker_registry=tracker_registry,
            transition_reporter=transition_reporter,
        )
        admission_coordinator = StopControlSubmissionAdmissionCoordinator(
            command_lock=command_lock,
            claim_registry=claim_registry,
            connection_gate=connection_gate,
            target_snapshot_factory=target_snapshot_factory,
            envelope_factory=envelope_factory,
            tracker_admission=tracker_admission,
            result_factory=result_factory,
        )
        self.coordinator = StopControlSubmissionCoordinator(
            admission_coordinator=admission_coordinator,
            prewrite_guard=StopControlPrewriteGuard(
                command_lock=command_lock,
                connection_ownership=connection_ownership,
                tracker_registry=tracker_registry,
                admission_barrier=admission_barrier,
            ),
            send_outcome_coordinator=StopControlSendOutcomeCoordinator(
                command_lock=command_lock,
                tracker_registry=tracker_registry,
                admission_barrier=admission_barrier,
                envelope_transport=envelope_transport,
                future_scheduler=future_scheduler,
                send_timeout_sec=send_timeout_sec,
                transition_reporter=transition_reporter,
                result_factory=result_factory,
            ),
            transition_reporter=transition_reporter,
            result_factory=result_factory,
        )


__all__ = ("StopControlSubmissionComponentGraph",)
