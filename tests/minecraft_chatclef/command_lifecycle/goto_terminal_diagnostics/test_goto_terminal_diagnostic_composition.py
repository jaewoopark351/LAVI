#20260913_kpopmodder: Exercise real late-terminal and immediate-coalesced diagnostic wiring.
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_component_graph import OrdinarySubmissionResultComponentGraph
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackAdmissionCoordinator, CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal.command_terminal_fact import CommandTerminalFact
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import FabricChatClefServerComponentGraph


class GotoTerminalDiagnosticCompositionTests(unittest.TestCase):
    def test_server_receives_and_renders_once_with_correlated_late_terminal_records(self):
        for status in ("completed", "failed"):
            with self.subTest(status=status):
                logs = []
                graph = _server_graph(logs)
                descriptor, grant, websocket, active = _bind(graph)
                responses = []
                graph.command_feedback_api.set_terminal_callback(responses.append)
                acknowledgement = graph.command_feedback_api.claim_start(grant, {
                    "ok": True,
                    "status": {"request_id": active.request_id, "status": "accepted", "ok": True, "data": {
                        "session_id": active.session_id, "connection_generation": active.generation,
                        "command_message_id": active.command_message_id,
                    }},
                })
                self.assertTrue(acknowledgement.acknowledge(published=True))
                graph.command_result_handler.handle(websocket, _envelope(active, "running"))
                terminal = _envelope(active, status)
                graph.command_result_handler.handle(websocket, terminal)
                graph.command_result_handler.handle(websocket, terminal)
                self.assertEqual(1, len(responses))
                self.assertEqual(descriptor.event_id, responses[0].event_id)
                self.assertIn("도착했는지는 확인하지 못했어" if status == "completed" else "가다가 실패했어", responses[0].text)
                records = [line for line in logs if "event=goto_terminal_diagnostic " in line]
                self.assertEqual(2, len(records))
                for line in records:
                    self.assertIn(
                        "role=result_evaluation" if "boundary=evidence_decided" in line else "role=late_terminal",
                        line,
                    )
                self.assertTrue(all("event_id=" + descriptor.event_id in line for line in records))
                self.assertTrue(all("request_id=" + active.request_id in line for line in records))
                self.assertTrue(all("correlation_id=" + active.command_message_id in line for line in records))
                self.assertTrue(any("boundary=response_rendered" in line for line in records))
                self.assertIsNone(graph.connection_ownership.active_command_owner)

    def test_immediate_submission_graph_injects_observer_into_actual_coalesced_factory(self):
        logs = []
        graph = OrdinarySubmissionResultComponentGraph(
            extension=Mock(), submission_boundary=Mock(), submission_reconciliation=Mock(),
            decision_factory=Mock(), live_proof_validator=Mock(), failure_handler=Mock(),
            router_logger=SimpleNamespace(log=logs.append),
        )
        fact = CommandTerminalFact(
            descriptor=_descriptor(), status="completed", verified=False,
            dispatch_started=False, result_reason="matching_task_finished", event_id="4" * 32,
            owner_token=SimpleNamespace(request_id="r", command_message_id="c", session_id="s", generation=1),
        )
        response = graph.start_decision_decorator._coalesced_responses.build(fact)
        self.assertEqual(CommandLifecycleResponseRenderer().render_terminal(fact), response.text)
        self.assertEqual(1, len(logs))
        self.assertIn("boundary=response_rendered owner=CommandLifecycleResponseRenderer role=immediate_coalesced", logs[0])
        self.assertIn("status=completed verified=false", logs[0])


def _server_graph(logs):
    return FabricChatClefServerComponentGraph(
        config=SimpleNamespace(reconcile_stale_deposit_to_unknown_enabled=False, startup_timeout_sec=1.0),
        session_registry=FabricChatClefSessionRegistry(),
        diagnostics=SimpleNamespace(info=logs.append, warning=logs.append),
        lifecycle=SimpleNamespace(stopping=False, loop=None, record_client_error=lambda _: None, bind=lambda **_: None),
        message_id_factory=lambda: "generated-message", now_ms=lambda: 1,
    )


def _descriptor():
    return CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
        "goto 500 90 -928", command_source="lavi_gui", event_id="4" * 32,
        provider_id="minecraft_fabric_chatclef_ui", event_kind="minecraft_raw_gui_submit",
    )


def _bind(graph):
    descriptor = _descriptor()
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda *_: False,
        descriptor_factory=CommandFeedbackDescriptorFactory(),
    ).issue_descriptor(descriptor)
    websocket = object()
    assert graph.connection_ownership.try_activate(websocket=websocket, session_id="goto-diagnostic-session").accepted
    assert graph.command_feedback_api.reserve(grant)
    with graph.command_lock:
        active = graph.connection_ownership.begin_command(
            request_id="goto-diagnostic-request", command_message_id="goto-diagnostic-message",
            command=descriptor.command, source=descriptor.command_source,
            metadata={"input_event": {
                "source": descriptor.input_source, "provider_id": descriptor.provider_id,
                "event_kind": descriptor.event_kind, "final": True, "event_id": descriptor.event_id,
            }},
        )
    assert active is not None
    return descriptor, grant, websocket, active


def _envelope(active, status):
    state = CommandResultStatus(status)
    result = CommandResultDTO(request_id=active.request_id, status=state, ok=state.ok, data={
        "result_reason": "dispatch_started" if status == "running" else "matching_task_finished",
        "evidence_sequence": 1 if status == "running" else 2,
        "result_fidelity": "callback_plus_matching_user_task_event",
    })
    return BridgeEnvelopeDTO(
        protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-" + status, correlation_id=active.command_message_id,
        session_id=active.session_id, timestamp_ms=1, payload=result.to_dict(),
    )
