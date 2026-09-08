#20260907_kpopmodder: Pin Python DTO and lifecycle ingestion to the shared Java initial-running wire fixture.
from __future__ import annotations

import json
import threading
import unittest
from pathlib import Path

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleFacade,
    CommandFeedbackResultCoordinator,
    CommandTerminalEvidenceEvaluator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_handler import (
    FabricChatClefCommandResultHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


FIXTURE_PATH = (
    Path(__file__).resolve().parent
    / "fixtures"
    / "fabric_chatclef_initial_running_command_result_v1.json"
)


class InitialRunningWireFixtureTests(unittest.TestCase):
    def test_python_dto_and_result_lifecycle_accept_and_record_java_fixture(self):
        raw_envelope = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
        envelope = BridgeEnvelopeDTO.from_mapping(raw_envelope)
        result = CommandResultDTO.from_mapping(envelope.payload)

        self.assertIs(BridgeMessageType.COMMAND_RESULT, envelope.message_type)
        self.assertIs(CommandResultStatus.RUNNING, result.status)
        self.assertIs(True, result.ok)
        self.assertEqual("dispatch_started", result.data["result_reason"])
        self.assertIs(type(result.data["evidence_sequence"]), int)
        self.assertEqual(1, result.data["evidence_sequence"])

        tracker, ownership, websocket, active = _bound_lifecycle()
        diagnostics = _Diagnostics()
        handler = FabricChatClefCommandResultHandler(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            diagnostics=diagnostics,
            crafting_feedback_result_coordinator=CommandFeedbackResultCoordinator(
                tracker=tracker,
                evidence_evaluator=CommandTerminalEvidenceEvaluator(),
            ),
        )

        handler.handle(websocket, envelope)

        self.assertIs(active, ownership.active_command_owner)
        self.assertTrue(tracker.dispatch_started_observed(active))
        snapshot = ownership.inspect_command_feedback_status()
        self.assertEqual(snapshot.RUNNING, snapshot.state)
        self.assertEqual("dispatch_started", snapshot.result_reason)
        self.assertTrue(snapshot.owner_present)
        self.assertEqual([], diagnostics.warnings)
        self.assertEqual(1, len(diagnostics.infos))

    def test_bool_evidence_sequence_is_accepted_as_transport_but_not_recorded(self):
        raw_envelope = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
        raw_envelope["payload"]["data"]["evidence_sequence"] = True
        envelope = BridgeEnvelopeDTO.from_mapping(raw_envelope)
        tracker, ownership, websocket, active = _bound_lifecycle()
        diagnostics = _Diagnostics()
        handler = FabricChatClefCommandResultHandler(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            diagnostics=diagnostics,
            crafting_feedback_result_coordinator=CommandFeedbackResultCoordinator(
                tracker=tracker,
                evidence_evaluator=CommandTerminalEvidenceEvaluator(),
            ),
        )

        handler.handle(websocket, envelope)

        self.assertIs(active, ownership.active_command_owner)
        self.assertFalse(tracker.dispatch_started_observed(active))
        snapshot = ownership.inspect_command_feedback_status()
        self.assertEqual(snapshot.PENDING, snapshot.state)
        self.assertEqual("", snapshot.result_reason)
        self.assertEqual([], diagnostics.warnings)
        self.assertEqual(1, len(diagnostics.infos))


def _bound_lifecycle():
    descriptor_factory = CommandFeedbackDescriptorFactory()
    descriptor = descriptor_factory.decode_registered_command_name_only(
        "@get diamond_pickaxe 1",
        command_source="lavi_gui",
        event_id="a" * 32,
        provider_id="minecraft_gui",
        event_kind="raw_command_submit",
    )
    if descriptor is None:
        raise AssertionError("fixture descriptor was not decoded")
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=descriptor_factory,
    ).issue_descriptor(descriptor)
    if grant is None:
        raise AssertionError("fixture feedback grant was not issued")

    tracker = CommandFeedbackLifecycleFacade()
    ownership = FabricChatClefConnectionOwnership(
        crafting_feedback_tracker=tracker,
    )
    websocket = object()
    admission = ownership.try_activate(
        websocket=websocket,
        session_id="session-fixture",
    )
    if not admission.accepted or not ownership.reserve_command_feedback(grant):
        raise AssertionError("fixture connection or feedback reservation failed")
    active = ownership.begin_command(
        request_id="request-fixture",
        command_message_id="command-message-fixture",
        command=descriptor.command,
        source=descriptor.command_source,
        metadata={
            "input_event": {
                "source": descriptor.input_source,
                "provider_id": descriptor.provider_id,
                "event_kind": descriptor.event_kind,
                "final": True,
                "event_id": descriptor.event_id,
            }
        },
    )
    if active is None or tracker.context is None:
        raise AssertionError("fixture command owner was not bound")
    return tracker, ownership, websocket, active


class _Diagnostics:
    def __init__(self) -> None:
        self.infos: list[str] = []
        self.warnings: list[str] = []

    def info(self, message: str) -> None:
        self.infos.append(message)

    def warning(self, message: str) -> None:
        self.warnings.append(message)


if __name__ == "__main__":
    unittest.main()
