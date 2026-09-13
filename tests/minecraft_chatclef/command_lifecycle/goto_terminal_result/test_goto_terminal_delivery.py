#20260913_kpopmodder: Exercise correlated GOTO results through production publication to Chat and TTS listener boundaries.
import unittest
from copy import deepcopy
from types import SimpleNamespace

from app_core.composition_core.component_wiring import MinecraftCommandLifecycleTerminalResponseWiring
from llm_core.output import LlmOutputListenerRegistry
from llm_core.routed_response import RoutedExternalResponsePublisher, RoutedResponseUiPresentationQueue
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackAdmissionCoordinator, CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.transport.control.stop import StopControlIdentity, StopControlTargetSnapshot
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_tracker import StopControlTracker
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import FabricChatClefServerComponentGraph

from .fixtures import binding_data, data, descriptor


class GotoTerminalDeliveryTests(unittest.TestCase):
    def test_late_chat_and_voice_arrival_reaches_ui_and_tts_listener_exactly_once(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            with self.subTest(source=source):
                fixture = _fixture(source)
                self.assertTrue(fixture.ack.acknowledge(published=True))
                _send_bound(fixture)
                terminal = _terminal(fixture)
                fixture.graph.command_result_handler.handle(fixture.websocket, terminal)
                fixture.graph.command_result_handler.handle(fixture.websocket, terminal)
                self.assert_one_delivery(fixture, "500, 80, -950 좌표에 도착했어.")
                self.assertIsNone(fixture.graph.connection_ownership.active_command_owner)
                self.assertTrue(any("reason=goto_arrival_verified" in line for line in fixture.logs))

    def test_typed_failure_survives_to_chat_and_tts_without_success(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            with self.subTest(source=source):
                fixture = _fixture(source)
                fixture.ack.acknowledge(published=True)
                _send_bound(fixture)
                terminal = _terminal(fixture, status="failed", failure_reason="HANDOFF_SHORTAGE")
                fixture.graph.command_result_handler.handle(fixture.websocket, terminal)
                fixture.graph.command_result_handler.handle(fixture.websocket, terminal)
                self.assert_one_delivery(fixture, "500, 80, -950 좌표로 이동하지 못했어. 이동을 다시 시작할 때 필요한 블록이 부족했어.")
                self.assertTrue(any("reason=goto_failure_verified" in line and "verified=false" in line for line in fixture.logs))

    def test_terminal_before_start_ack_remains_deferred_for_finite_goto(self):
        fixture = _fixture("lavi_chat_ui")
        _send_bound(fixture)
        fixture.graph.command_result_handler.handle(fixture.websocket, _terminal(fixture))
        self.assertEqual([], fixture.tts_payloads)
        self.assertEqual((), fixture.ui_queue.snapshot()[1])
        self.assertIsNone(fixture.ack.select_coalesced_terminal())
        self.assertTrue(fixture.ack.acknowledge(published=True))
        self.assert_one_delivery(fixture, "500, 80, -950 좌표에 도착했어.")
        fixture.graph.command_result_handler.handle(fixture.websocket, _terminal(fixture))
        self.assertEqual(1, len(fixture.tts_payloads))

    def test_missing_or_conflicting_running_owner_keeps_cautious_sentence(self):
        for conflicting in (False, True):
            with self.subTest(conflicting=conflicting):
                fixture = _fixture("lavi_chat_ui")
                fixture.ack.acknowledge(published=True)
                if conflicting:
                    _send_bound(fixture)
                    changed = deepcopy(fixture.binding)
                    changed["operation_id"] = "foreign-operation"
                    _send_bound(fixture, binding=changed, sequence=2)
                fixture.graph.command_result_handler.handle(fixture.websocket, _terminal(fixture, sequence=3))
                self.assert_one_delivery(fixture, "좌표 500, 80, -950으로 가는 작업은 끝났는데, 도착했는지는 확인하지 못했어")

    def test_stale_outer_session_and_foreign_socket_cannot_publish_current_arrival(self):
        fixture = _fixture("lavi_chat_ui")
        fixture.ack.acknowledge(published=True)
        _send_bound(fixture)
        terminal = _terminal(fixture)
        stale = BridgeEnvelopeDTO(
            protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
            message_id="stale-result", correlation_id=fixture.active.command_message_id,
            session_id="stale-session", timestamp_ms=1, payload=terminal.payload,
        )
        fixture.graph.command_result_handler.handle(fixture.websocket, stale)
        fixture.graph.command_result_handler.handle(object(), terminal)
        self.assertEqual([], fixture.tts_payloads)
        fixture.graph.command_result_handler.handle(fixture.websocket, terminal)
        self.assert_one_delivery(fixture, "500, 80, -950 좌표에 도착했어.")

    def test_user_cancel_wins_over_later_arrival(self):
        fixture = _fixture("lavi_chat_ui")
        fixture.ack.acknowledge(published=True)
        _send_bound(fixture)
        cancelled = _terminal(fixture, status="cancelled", reason="user_stop_requested")
        fixture.graph.command_result_handler.handle(fixture.websocket, cancelled)
        fixture.graph.command_result_handler.handle(fixture.websocket, _terminal(fixture, sequence=3))
        self.assert_one_delivery(fixture, "위치 이동 작업이 중단됐어")

    def test_specialized_stop_owner_suppresses_original_goto_terminal_and_late_arrival(self):
        fixture = _fixture("voice_input_final")
        fixture.ack.acknowledge(published=True)
        _send_bound(fixture)
        active = fixture.active
        stop = StopControlTracker(
            identity=StopControlIdentity(request_id="stop-request", message_id="stop-message",
                                         session_id=active.session_id, server_connection_generation=active.generation),
            barrier_token=object(), transport_websocket=fixture.websocket,
            request=SimpleNamespace(metadata={"target_scope": "tracked_command"}),
            event_id="b" * 32, source="voice_input_final", target_scope="tracked_command",
            target=StopControlTargetSnapshot(request_id=active.request_id,
                command_message_id=active.command_message_id, session_id=active.session_id,
                server_connection_generation=active.generation, owner_token=active),
        )
        self.assertTrue(fixture.graph.stop_control_tracker_registry.register(stop))
        fixture.graph.command_result_handler.handle(fixture.websocket,
            _terminal(fixture, status="cancelled", reason="user_stop_requested"))
        fixture.graph.command_result_handler.handle(fixture.websocket, _terminal(fixture, sequence=3))
        self.assertEqual([], fixture.tts_payloads)
        self.assertEqual((), fixture.ui_queue.snapshot()[1])

    def assert_one_delivery(self, fixture, text):
        self.assertEqual(1, len(fixture.output_payloads))
        self.assertEqual(fixture.output_payloads, fixture.tts_payloads)
        self.assertEqual(text, fixture.tts_payloads[0]["text"])
        entries = fixture.ui_queue.snapshot()[1]
        self.assertEqual(1, len(entries))
        self.assertEqual(text, entries[0].content)
        self.assertEqual([], fixture.generations)


def _fixture(source):
    logs, output_payloads, tts_payloads, generations = [], [], [], []
    registry = LlmOutputListenerRegistry()
    registry.add(output_payloads.append)
    registry.add(tts_payloads.append)
    ui_queue = RoutedResponseUiPresentationQueue()
    publisher = RoutedExternalResponsePublisher(
        begin_generation_callback=lambda: generations.append(1) or 1,
        build_output_payload_callback=lambda text, generation: {"text": text, "response_generation": generation},
        send_output_callback=registry.send_output, send_full_output_callback=lambda _text: None,
        ui_presentation_callback=ui_queue.enqueue, log_callback=logs.append,
    )
    graph = FabricChatClefServerComponentGraph(
        config=SimpleNamespace(reconcile_stale_deposit_to_unknown_enabled=False, startup_timeout_sec=1.0),
        session_registry=FabricChatClefSessionRegistry(),
        diagnostics=SimpleNamespace(info=logs.append, warning=logs.append),
        lifecycle=SimpleNamespace(stopping=False, loop=None, record_client_error=lambda _: None, bind=lambda **_: None),
        message_id_factory=lambda: "generated-message", now_ms=lambda: 1,
    )
    MinecraftCommandLifecycleTerminalResponseWiring().wire(llm=publisher, extension=SimpleNamespace(
        set_command_lifecycle_terminal_response_callback=graph.command_feedback_api.set_terminal_callback,
    ))
    desc = descriptor(source)
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda *_: False, descriptor_factory=CommandFeedbackDescriptorFactory(),
    ).issue_descriptor(desc)
    websocket = object()
    assert graph.connection_ownership.try_activate(websocket=websocket, session_id="session-goto").accepted
    assert graph.command_feedback_api.reserve(grant)
    with graph.command_lock:
        active = graph.connection_ownership.begin_command(
            request_id="request-goto", command_message_id="message-goto",
            command=desc.command, source=desc.command_source,
            metadata={"input_event": {"source": desc.input_source, "provider_id": desc.provider_id,
                "event_kind": desc.event_kind, "final": True, "event_id": desc.event_id}},
        )
    assert active is not None
    ack = graph.command_feedback_api.claim_start(grant, {
        "ok": True, "status": {"request_id": active.request_id, "status": "accepted", "ok": True,
            "data": {"session_id": active.session_id, "connection_generation": active.generation,
                     "command_message_id": active.command_message_id}},
    })
    assert ack is not None
    return SimpleNamespace(graph=graph, websocket=websocket, active=active, ack=ack, logs=logs,
        binding=binding_data(server_connection_generation=active.generation), ui_queue=ui_queue,
        output_payloads=output_payloads, tts_payloads=tts_payloads, generations=generations)


def _send_bound(fixture, *, binding=None, sequence=1):
    payload = {"result_reason": "dispatch_started", "evidence_sequence": sequence,
        "result_fidelity": "dispatch_started_only",
        "goto_profile_id": "fabric_chatclef_goto_terminal", "goto_profile_version": 1,
        "goto_binding": binding or fixture.binding}
    fixture.graph.command_result_handler.handle(fixture.websocket, _envelope(fixture, "running", payload))


def _terminal(fixture, *, status="completed", failure_reason="NONE", reason="matching_task_finished", sequence=2):
    payload = data(**fixture.binding)
    payload["goto_binding"] = dict(fixture.binding)
    payload["evidence_sequence"] = sequence
    payload["result_reason"] = reason
    if status == "failed":
        payload["goto_terminal"].update(outcome="FAILED", goal_satisfied=False, failure_reason=failure_reason)
    return _envelope(fixture, status, payload)


def _envelope(fixture, status, payload):
    state = CommandResultStatus(status)
    command_result = CommandResultDTO(request_id=fixture.active.request_id, status=state, ok=state.ok,
        error_code="internal_error" if status == "failed" else None, data=payload)
    return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-" + status, correlation_id=fixture.active.command_message_id,
        session_id=fixture.active.session_id, timestamp_ms=1, payload=command_result.to_dict())


if __name__ == "__main__":
    unittest.main()
