#20260905_kpopmodder: Assemble focused STOP send-outcome stages outside the sequencer.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_diagnostic_stage import (
    StopControlSendDiagnosticStage,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_result_stage import StopControlSendResultStage
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_state_transition_stage import (
    StopControlSendStateTransitionStage,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_transport_delivery_stage import (
    StopControlTransportDeliveryStage,
)


class StopControlSendOutcomeComponentGraph:
    def __init__(
        self,
        *,
        command_lock: object,
        tracker_registry: object,
        admission_barrier: object,
        envelope_transport: object,
        future_scheduler: object,
        send_timeout_sec: float,
        transition_reporter: object,
        result_factory: object,
    ):
        self.transport_delivery = StopControlTransportDeliveryStage(
            envelope_transport=envelope_transport,
            future_scheduler=future_scheduler,
            send_timeout_sec=send_timeout_sec,
        )
        self.state_transitions = StopControlSendStateTransitionStage(
            command_lock=command_lock,
            tracker_registry=tracker_registry,
            admission_barrier=admission_barrier,
        )
        self.diagnostics = StopControlSendDiagnosticStage(
            transition_reporter
        )
        self.results = StopControlSendResultStage(result_factory)


__all__ = ("StopControlSendOutcomeComponentGraph",)
