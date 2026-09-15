# 20260915_kpopmodder: The original request's lifecycle survives a separate confirmation event and reaches UI/TTS listeners once.
import pytest
from app_core.composition_core.component_wiring import (
    MinecraftCommandLifecycleTerminalResponseWiring,
)
from llm_core.output import LlmOutputListenerRegistry
from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponseUiPresentationQueue,
)
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from .lifecycle_adapter_fixture import ConfirmationLifecycleAdapter
from .trusted_route_fixture import TrustedConfirmationRouteFixture


@pytest.mark.parametrize("voice", (False, True))
def test_confirmed_follow_start_and_late_cancel_preserve_original_owner_and_deliver_once(
    voice,
):
    adapter = ConfirmationLifecycleAdapter()
    f = TrustedConfirmationRouteFixture(adapter=adapter)
    outputs, speech = [], []
    listeners = LlmOutputListenerRegistry()
    listeners.add(outputs.append)
    listeners.add(speech.append)
    ui = RoutedResponseUiPresentationQueue()
    publisher = RoutedExternalResponsePublisher(
        begin_generation_callback=lambda: 1,
        build_output_payload_callback=lambda text, generation: {
            "text": text,
            "response_generation": generation,
        },
        send_output_callback=listeners.send_output,
        send_full_output_callback=lambda _: None,
        ui_presentation_callback=ui.enqueue,
        log_callback=adapter.logs.append,
    )
    MinecraftCommandLifecycleTerminalResponseWiring().wire(
        llm=publisher, extension=f.extension
    )
    prompt, _ = f.dispatch("Alex 따라가줘", voice=voice)
    assert len(prompt) == 1 and "확인" in prompt[0]
    start, _ = f.dispatch("확인", voice=voice)
    assert len(adapter.requests) == 1 and len(start) == 1
    assert "Alex" in getattr(start[0], "content", start[0])
    assert adapter.graph.connection_ownership.active_command_owner is not None
    request, active = adapter.requests[0], adapter.active
    assert (
        request.metadata["korean_confirmation"]["original_event_id"]
        == request.metadata["input_event"]["event_id"]
    )
    result = CommandResultDTO(
        request_id=active.request_id,
        status="cancelled",
        ok=False,
        data={"result_reason": "user_stop_requested", "evidence_sequence": 2},
    )
    envelope = BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="confirmed-result",
        correlation_id=active.command_message_id,
        session_id=active.session_id,
        timestamp_ms=1,
        payload=result.to_dict(),
    )
    adapter.graph.command_result_handler.handle(adapter.websocket, envelope)
    adapter.graph.command_result_handler.handle(adapter.websocket, envelope)
    assert len(outputs) == len(speech) == len(ui.snapshot()[1]) == 1
    assert outputs == speech and "중단" in speech[0]["text"]
    assert adapter.graph.connection_ownership.active_command_owner is None
    assert f.pipeline.provider_calls == 0
