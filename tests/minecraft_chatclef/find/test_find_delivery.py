#20260914_kpopmodder: Verify FIND terminal acceptance through production UI and TTS-listener publication.
from types import SimpleNamespace
import unittest

from app_core.composition_core.component_wiring import MinecraftCommandLifecycleTerminalResponseWiring
from llm_core.output import LlmOutputListenerRegistry
from llm_core.routed_response import RoutedExternalResponsePublisher, RoutedResponseUiPresentationQueue
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackAdmissionCoordinator, CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import FabricChatClefServerComponentGraph

from .fixtures import data, descriptor


def fixture(source):
    logs, output, tts = [], [], []
    listeners = LlmOutputListenerRegistry()
    listeners.add(output.append)
    listeners.add(tts.append)
    ui = RoutedResponseUiPresentationQueue()
    publisher = RoutedExternalResponsePublisher(begin_generation_callback=lambda: 1,
        build_output_payload_callback=lambda text, generation: {"text": text, "response_generation": generation},
        send_output_callback=listeners.send_output, send_full_output_callback=lambda _: None,
        ui_presentation_callback=ui.enqueue, log_callback=logs.append)
    graph = FabricChatClefServerComponentGraph(
        config=SimpleNamespace(reconcile_stale_deposit_to_unknown_enabled=False, startup_timeout_sec=1.0),
        session_registry=FabricChatClefSessionRegistry(), diagnostics=SimpleNamespace(info=logs.append, warning=logs.append),
        lifecycle=SimpleNamespace(stopping=False, loop=None, record_client_error=lambda _: None, bind=lambda **_: None),
        message_id_factory=lambda: "find-generated", now_ms=lambda: 1)
    MinecraftCommandLifecycleTerminalResponseWiring().wire(llm=publisher, extension=SimpleNamespace(
        set_command_lifecycle_terminal_response_callback=graph.command_feedback_api.set_terminal_callback))
    desc = descriptor(source)
    grant = CommandFeedbackAdmissionCoordinator(live_proof_validator=lambda *_: False,
        descriptor_factory=CommandFeedbackDescriptorFactory()).issue_descriptor(desc)
    websocket = object()
    assert graph.connection_ownership.try_activate(websocket=websocket, session_id="find-session").accepted
    assert graph.command_feedback_api.reserve(grant)
    active = graph.connection_ownership.begin_command(request_id="find-request", command_message_id="find-message",
        command=desc.command, source=desc.command_source, metadata={"input_event": dict(source=desc.input_source,
        provider_id=desc.provider_id, event_kind=desc.event_kind, final=True, event_id=desc.event_id)})
    ack = graph.command_feedback_api.claim_start(grant, {"ok": True, "status": dict(request_id=active.request_id,
        status="accepted", ok=True, data=dict(session_id=active.session_id, connection_generation=active.generation,
        command_message_id=active.command_message_id))})
    assert ack is not None
    return SimpleNamespace(graph=graph, websocket=websocket, active=active, ack=ack, output=output, tts=tts, ui=ui, logs=logs)


def envelope(current, result="FOUND_AND_REPORTED", status="completed", *, sequence=2, reason="matching_task_finished", session=None):
    payload = data(result)
    payload.update(evidence_sequence=sequence, result_reason=reason)
    state = CommandResultStatus(status)
    record = CommandResultDTO(request_id=current.active.request_id, status=state, ok=state.ok,
        error_code="internal_error" if status == "failed" else None, data=payload)
    return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
        message_id=f"find-terminal-{sequence}", correlation_id=current.active.command_message_id,
        session_id=session or current.active.session_id, timestamp_ms=1, payload=record.to_dict())


class FindDeliveryTests(unittest.TestCase):
    def test_chat_final_voice_found_miss_and_bounds_reach_ui_and_tts_listener_exactly_once(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            for outcome, status, phrase in (("FOUND_AND_REPORTED", "completed", "확인한 위치는 X -2"),
                    ("NOT_OBSERVED_IN_LOADED_SCOPE", "completed", "불러온 범위에서는"),
                    ("OBSERVATION_BOUNDS_EXHAUSTED", "failed", "탐색 한도")):
                with self.subTest(source=source, outcome=outcome):
                    current = fixture(source)
                    current.ack.acknowledge(published=True)
                    terminal = envelope(current, outcome, status)
                    current.graph.command_result_handler.handle(current.websocket, terminal)
                    current.graph.command_result_handler.handle(current.websocket, terminal)
                    self.assertEqual(1, len(current.output))
                    self.assertEqual(current.output, current.tts)
                    self.assertIn(phrase, current.tts[0]["text"])
                    self.assertEqual(1, len(current.ui.snapshot()[1]))
                    self.assertIsNone(current.graph.connection_ownership.active_command_owner)

    def test_completed_negative_query_is_deferred_until_start_ack_without_becoming_success(self):
        current = fixture("lavi_chat_ui")
        current.graph.command_result_handler.handle(current.websocket, envelope(current, "NOT_OBSERVED_IN_LOADED_SCOPE"))
        self.assertEqual([], current.output)
        current.ack.acknowledge(published=True)
        self.assertEqual(1, len(current.output))
        self.assertIn("확인하지 못했어", current.output[0]["text"])
        self.assertNotIn("찾았어", current.output[0]["text"])

    def test_callback_only_result_is_cautious_and_stale_socket_session_are_rejected(self):
        current = fixture("lavi_chat_ui")
        current.ack.acknowledge(published=True)
        current.graph.command_result_handler.handle(object(), envelope(current))
        current.graph.command_result_handler.handle(current.websocket, envelope(current, session="foreign"))
        self.assertEqual([], current.output)
        current.graph.command_result_handler.handle(current.websocket, envelope(current, reason="command_finish_callback_only"))
        self.assertEqual(1, len(current.output))
        self.assertNotIn("찾았어", current.output[0]["text"])

    def test_user_cancel_retires_owner_and_late_found_cannot_overwrite_response(self):
        current = fixture("voice_input_final")
        current.ack.acknowledge(published=True)
        current.graph.command_result_handler.handle(current.websocket, envelope(current, status="cancelled", reason="user_stop_requested"))
        current.graph.command_result_handler.handle(current.websocket, envelope(current, sequence=3))
        self.assertEqual(1, len(current.output))
        self.assertIn("중단됐어", current.output[0]["text"])
        self.assertNotIn("찾았어", current.output[0]["text"])
