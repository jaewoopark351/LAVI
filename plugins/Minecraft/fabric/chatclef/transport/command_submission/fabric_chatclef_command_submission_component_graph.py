#20260905_kpopmodder: Assemble the focused ordinary-command submission component graph.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_submission.delivery import (
    FabricChatClefCommandDelivery,
    FabricChatClefCommandTransportDeliveryStage,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.diagnostics import (
    FabricChatClefCommandSubmissionDiagnostics,
    FabricChatClefCommandSubmissionEventDetailsFactory,
    FabricChatClefCommandSubmissionEventReporter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_envelope_factory import (
    FabricChatClefCommandEnvelopeFactory,
    default_fabric_chatclef_message_id,
    system_now_ms,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_admission import FabricChatClefCommandAdmission
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_submission_sequence import FabricChatClefCommandSubmissionSequence
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_delivery_outcome_policy import FabricChatClefCommandDeliveryOutcomePolicy
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_factory import FabricChatClefCommandResultFactory


class FabricChatClefCommandSubmissionComponentGraph:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        diagnostics,
        loop_provider,
        envelope_transport,
        send_timeout_sec: float,
        future_scheduler,
        message_id_factory=None,
        now_ms=None,
        stop_control_admission_barrier=None,
    ) -> None:
        self.loop_provider = loop_provider
        self.envelope_transport = envelope_transport
        self.message_id_factory = (
            message_id_factory or default_fabric_chatclef_message_id
        )
        self.now_ms = now_ms or system_now_ms
        self.envelope_factory = FabricChatClefCommandEnvelopeFactory(
            message_id_factory=self.message_id_factory,
            now_ms=self.now_ms,
        )
        self.admission = FabricChatClefCommandAdmission(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            now_ms=self.now_ms,
            stop_control_admission_barrier=stop_control_admission_barrier,
        )
        self.delivery = FabricChatClefCommandDelivery(
            future_scheduler=future_scheduler,
            send_timeout_sec=send_timeout_sec,
        )
        self.transport_delivery = FabricChatClefCommandTransportDeliveryStage(
            envelope_transport=envelope_transport,
            delivery=self.delivery,
        )
        self.results = FabricChatClefCommandResultFactory()
        self.events = FabricChatClefCommandSubmissionDiagnostics(diagnostics)
        self.event_details_factory = (
            FabricChatClefCommandSubmissionEventDetailsFactory(now_ms=self.now_ms)
        )
        self.event_reporter = FabricChatClefCommandSubmissionEventReporter(
            diagnostics=self.events,
            details_factory=self.event_details_factory,
        )
        self.outcome_policy = FabricChatClefCommandDeliveryOutcomePolicy()
        self.sequence = FabricChatClefCommandSubmissionSequence(
            loop_provider=loop_provider,
            envelope_factory=self.envelope_factory,
            admission=self.admission,
            transport_delivery=self.transport_delivery,
            outcome_policy=self.outcome_policy,
            event_reporter=self.event_reporter,
            result_factory=self.results,
        )
