#20260905_kpopmodder: Verify focused command-submission stages and legacy compatibility paths.
from __future__ import annotations

import asyncio
import threading
import unittest

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.admission import (
    FabricChatClefOrdinaryAdmissionInspector,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.delivery import (
    FabricChatClefCommandDelivery as CanonicalDelivery,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_admission import (
    FabricChatClefCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_delivery import (
    FabricChatClefCommandDelivery as LegacyDelivery,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_factory import (
    FabricChatClefCommandResultFactory as LegacyResultFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_submitter import (
    FabricChatClefCommandSubmitter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_factory import (
    FabricChatClefCommandResultFactory as CanonicalResultFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlAdmissionBarrier,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class CommandSubmissionStructureTests(unittest.TestCase):
    def test_read_only_inspector_never_claims_ordinary_command_ownership(self):
        ownership = _connected_ownership()
        inspector = FabricChatClefOrdinaryAdmissionInspector(
            connection_ownership=ownership,
            now_ms=lambda: 1,
        )

        inspection = inspector.inspect(_request(), loop=_RunningLoop())

        self.assertTrue(inspection.allowed)
        self.assertIsNone(ownership.active_request_id)

    def test_admission_facade_commits_only_after_allowed_inspection(self):
        ownership = _connected_ownership()
        admission = FabricChatClefCommandAdmission(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            now_ms=lambda: 1,
        )

        decision = admission.inspect(
            _request(),
            message_id="message-1",
            loop=_RunningLoop(),
        )

        self.assertTrue(decision.accepted)
        self.assertEqual("request-1", ownership.active_request_id)
        self.assertEqual(
            "FabricChatClefOrdinaryAdmissionInspector",
            type(admission._components.inspector).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandOwnershipCommitter",
            type(admission._components.ownership_committer).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandAdmissionDecisionFactory",
            type(admission._components.decision_factory).__name__,  # noqa: SLF001
        )

    def test_stop_barrier_rejects_before_ownership_commit(self):
        ownership = _connected_ownership()
        barrier = StopControlAdmissionBarrier()
        self.assertIsNotNone(barrier.close(("stop-request",)))
        admission = FabricChatClefCommandAdmission(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            now_ms=lambda: 1,
            stop_control_admission_barrier=barrier,
        )

        decision = admission.inspect(
            _request(),
            message_id="message-1",
            loop=_RunningLoop(),
        )

        self.assertFalse(decision.accepted)
        self.assertEqual("command_rejected_stop_control_barrier", decision.event)
        self.assertIsNone(ownership.active_request_id)

    def test_submitter_facade_delegates_to_focused_component_graph(self):
        submitter = FabricChatClefCommandSubmitter(
            connection_ownership=_connected_ownership(),
            command_lock=threading.RLock(),
            diagnostics=_Diagnostics(),
            loop_provider=_RunningLoop,
            envelope_transport=_EnvelopeTransport(),
            send_timeout_sec=0.01,
            message_id_factory=lambda: "message-1",
            now_ms=lambda: 1,
        )

        self.assertEqual(
            "FabricChatClefCommandSubmissionSequence",
            type(submitter._components.sequence).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandEnvelopeFactory",
            type(submitter._components.envelope_factory).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandTransportDeliveryStage",
            type(submitter._components.transport_delivery).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandDeliveryOutcomePolicy",
            type(submitter._components.outcome_policy).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandSubmissionEventReporter",
            type(submitter._components.event_reporter).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefCommandResultFactory",
            type(submitter._components.results).__name__,  # noqa: SLF001
        )

    def test_submission_sequence_preserves_accepted_wire_and_diagnostics(self):
        transport = _EnvelopeTransport()
        diagnostics = _Diagnostics()
        submitter = FabricChatClefCommandSubmitter(
            connection_ownership=_connected_ownership(),
            command_lock=threading.RLock(),
            diagnostics=diagnostics,
            loop_provider=_RunningLoop,
            envelope_transport=transport,
            send_timeout_sec=0.01,
            future_scheduler=_run_immediately,
            message_id_factory=lambda: "message-1",
            now_ms=lambda: 1,
        )

        result = submitter.submit(_request())

        self.assertEqual(CommandResultStatus.ACCEPTED, result.status)
        self.assertEqual(1, len(transport.envelopes))
        envelope = transport.envelopes[0]
        self.assertEqual("message-1", envelope.message_id)
        self.assertEqual("request-1", envelope.correlation_id)
        self.assertEqual("request-1", envelope.payload["request_id"])
        self.assertEqual(2, len(diagnostics.messages))
        self.assertIn('"event":"command_accepted"', diagnostics.messages[0])
        self.assertIn('"event":"command_send_succeeded"', diagnostics.messages[1])

    def test_legacy_delivery_and_result_imports_resolve_canonical_types(self):
        self.assertIs(LegacyDelivery, CanonicalDelivery)
        self.assertIs(LegacyResultFactory, CanonicalResultFactory)


class _RunningLoop:
    @staticmethod
    def is_running() -> bool:
        return True


class _EnvelopeTransport:
    def __init__(self) -> None:
        self.envelopes = []

    async def send(self, _websocket, _envelope) -> None:
        self.envelopes.append(_envelope)


class _Diagnostics:
    def __init__(self) -> None:
        self.messages: list[str] = []

    def info(self, message) -> None:
        self.messages.append(message)


class _CompletedFuture:
    @staticmethod
    def result(*, timeout) -> None:
        del timeout


def _run_immediately(coroutine, _loop):
    asyncio.run(coroutine)
    return _CompletedFuture()


def _connected_ownership() -> FabricChatClefConnectionOwnership:
    ownership = FabricChatClefConnectionOwnership()
    admission = ownership.try_activate(websocket=object(), session_id="session-1")
    if not admission.accepted:
        raise AssertionError("fixture connection was not accepted")
    return ownership


def _request() -> CommandRequestDTO:
    return CommandRequestDTO(
        request_id="request-1",
        command="get stone 1",
        source="unit-test",
    )


if __name__ == "__main__":
    unittest.main()
