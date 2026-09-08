#20260908_kpopmodder: Prove production server composition owns STATUS diagnostic custody and cleanup.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleTerminalResponse,
)
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import (
    FabricChatClefSessionRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status import (
    CommandStatusFailureDiagnosticCustody,
    CommandStatusFailureDiagnosticCustodyFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import (
    FabricChatClefServerComponentGraph,
)


class FabricServerGraphStatusDiagnosticCustodyTests(unittest.TestCase):
    def test_real_graph_status_custody_and_false_ack_release_staged_terminal(self):
        diagnostics = _Diagnostics()
        graph = _graph(diagnostics)
        descriptor, grant, websocket, active = _bind_ordinary_command(graph)
        delivered = []
        graph.command_feedback_api.set_terminal_callback(delivered.append)

        start_acknowledgement = graph.command_feedback_api.claim_start(
            grant,
            _accepted_submission_result(active),
        )
        self.assertIs(
            type(start_acknowledgement),
            CommandFeedbackPublicationAcknowledgement,
        )
        self.assertTrue(start_acknowledgement.acknowledge(published=True))

        graph.command_result_handler.handle(
            websocket,
            _envelope(active, _running_result(active)),
        )
        self.assertIs(active, graph.connection_ownership.active_command_owner)

        query = CommandStatusQuery("any", False)
        snapshot = graph.command_feedback_api.inspect_status(query)

        self.assertIs(type(snapshot), CommandFeedbackLifecycleSnapshot)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.RUNNING, snapshot.state)
        self.assertEqual("dispatch_started", snapshot.result_reason)
        self.assertIs(descriptor, snapshot.descriptor)
        acknowledgement = snapshot.publication_acknowledgement
        self.assertIs(
            type(acknowledgement),
            CommandFeedbackPublicationAcknowledgement,
        )
        self.assertEqual("status", acknowledgement.kind)

        factory = graph.command_status_failure_diagnostic_custody_factory
        self.assertIs(type(factory), CommandStatusFailureDiagnosticCustodyFactory)
        injected_factory = (
            graph.command_feedback_api
            ._status_publication_handoff
            ._diagnostic_custody_factory
        )
        self.assertIs(factory, injected_factory.__self__)
        custody = acknowledgement.publication_failure_diagnostic_custody
        self.assertIs(type(custody), CommandStatusFailureDiagnosticCustody)
        self.assertIs(factory._record_callback, custody._record_callback)
        self.assertEqual("generic", custody._base_record.query_kind)
        self.assertEqual("any", custody._base_record.requested_family)
        self.assertEqual("get", custody._base_record.active_command_name)
        self.assertEqual("running", custody._base_record.lifecycle_state)
        self.assertEqual("unclaimed", custody._base_record.terminal_state)

        graph.command_result_handler.handle(
            websocket,
            _envelope(active, _failed_result(active)),
        )
        self.assertEqual([], delivered)

        self.assertTrue(acknowledgement.acknowledge(published=False))
        self.assertEqual(1, len(delivered))
        self.assertIs(type(delivered[0]), CommandLifecycleTerminalResponse)
        self.assertEqual(descriptor.command_name, delivered[0].command_name)
        self.assertEqual(descriptor.event_id, delivered[0].event_id)
        self.assertFalse(acknowledgement.acknowledge(published=False))

        after = graph.command_feedback_api.inspect_status(
            CommandStatusQuery("any", True)
        )
        self.assertIs(type(after), CommandFeedbackLifecycleSnapshot)
        self.assertIsNone(after.publication_acknowledgement)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, after.state)


class _Diagnostics:
    def __init__(self) -> None:
        self.infos: list[str] = []
        self.warnings: list[str] = []

    def info(self, message: str) -> None:
        self.infos.append(message)

    def warning(self, message: str) -> None:
        self.warnings.append(message)


def _graph(diagnostics: _Diagnostics) -> FabricChatClefServerComponentGraph:
    lifecycle = SimpleNamespace(
        stopping=False,
        loop=None,
        record_client_error=lambda _error: None,
        bind=lambda **_values: None,
    )
    return FabricChatClefServerComponentGraph(
        config=SimpleNamespace(
            reconcile_stale_deposit_to_unknown_enabled=False,
            startup_timeout_sec=1.0,
        ),
        session_registry=FabricChatClefSessionRegistry(),
        diagnostics=diagnostics,
        lifecycle=lifecycle,
        message_id_factory=lambda: "generated-message",
        now_ms=lambda: 1,
    )


def _bind_ordinary_command(graph: FabricChatClefServerComponentGraph):
    descriptor_factory = CommandFeedbackDescriptorFactory()
    descriptor = descriptor_factory.decode_registered_command_name_only(
        "get iron_ingot 2",
        command_source="lavi_gui",
        event_id="4" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )
    if descriptor is None:
        raise AssertionError("ordinary descriptor fixture was not decoded")
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=descriptor_factory,
    ).issue_descriptor(descriptor)
    if grant is None:
        raise AssertionError("ordinary feedback grant was not issued")
    websocket = object()
    admission = graph.connection_ownership.try_activate(
        websocket=websocket,
        session_id="graph-status-session",
    )
    if not admission.accepted or not graph.command_feedback_api.reserve(grant):
        raise AssertionError("graph connection or feedback reservation failed")
    with graph.command_lock:
        active = graph.connection_ownership.begin_command(
            request_id="graph-status-request",
            command_message_id="graph-status-command-message",
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
    if active is None:
        raise AssertionError("ordinary active command fixture was not bound")
    return descriptor, grant, websocket, active


def _accepted_submission_result(active: object) -> dict[str, object]:
    return {
        "ok": True,
        "status": {
            "request_id": active.request_id,
            "ok": True,
            "status": "accepted",
            "data": {
                "session_id": active.session_id,
                "connection_generation": active.generation,
                "command_message_id": active.command_message_id,
            },
        },
    }


def _running_result(active: object) -> CommandResultDTO:
    return CommandResultDTO(
        request_id=active.request_id,
        ok=True,
        status=CommandResultStatus.RUNNING,
        data={
            "result_reason": "dispatch_started",
            "evidence_sequence": 1,
        },
    )


def _failed_result(active: object) -> CommandResultDTO:
    return CommandResultDTO(
        request_id=active.request_id,
        ok=False,
        status=CommandResultStatus.FAILED,
        data={"result_reason": "task_failed"},
    )


def _envelope(active: object, result: CommandResultDTO) -> BridgeEnvelopeDTO:
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id=f"result-{result.status.value}",
        correlation_id=active.command_message_id,
        session_id=active.session_id,
        timestamp_ms=1,
        payload=result.to_dict(),
    )


if __name__ == "__main__":
    unittest.main()
