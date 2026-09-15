#20260915_kpopmodder: Exercise trusted-list UI and filtered speech through real result/publication owners.
from copy import deepcopy
from dataclasses import replace
from io import StringIO
import logging
from types import MethodType, SimpleNamespace

import pytest

from app_core.composition_core.component_wiring.minecraft_command_lifecycle_terminal_response_wiring import MinecraftCommandLifecycleTerminalResponseWiring
from app_core.composition_core.component_wiring.minecraft_lifecycle_tts_receipt_wiring import MinecraftLifecycleTtsReceiptWiring
from llm_core.routed_response import RoutedExternalResponsePublisher, RoutedResponseUiPresentationQueue
from llm_core.routed_response.presentation.ui.routed_response_ui_presentation_drain import RoutedResponseUiPresentationDrain
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal.instant.list_output.trusted_destination_list_speech_projector import TrustedDestinationListSpeechProjector
from tests.minecraft_chatclef.lavi_input.confirmation.lifecycle_adapter_fixture import ConfirmationLifecycleAdapter
from tests.minecraft_chatclef.lavi_input.confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from tests.tts_core.delivery.lifecycle_response import test_tts_lifecycle_response_component_integration as tts_fixture
from tts_core import TTS
from tts_core.text_processor import TTSTextProcessor

IDENTIFIER = "td_be0062c83f14cb8bd68b64b9"
PHRASE = "자동 보관 장소 목록 보여 줘"


def values(count=1, *, total=None):
    total = count if total is None else total
    return {"total": total, "listed": count, "truncated": count < total,
            "destinations": [{"destination_id": IDENTIFIER if i == 0 else f"td_{i:024x}",
                              "dimension": "overworld", "x": -746 - i, "y": 70, "z": 66,
                              "status": "UNKNOWN_OR_STALE"} for i in range(count)]}


class ListAdapter(ConfirmationLifecycleAdapter):
    def __init__(self, *, immediate=False, list_values=None):
        super().__init__()
        self.immediate = immediate
        self.list_values = values() if list_values is None else list_values

    def terminal(self, *, status="completed", data=None, session=None, request_id=None):
        active = self.active
        if data is None:
            data = {"result_reason": "instant_command_observed",
                    "result_fidelity": "command_callback_plus_native_result", "evidence_sequence": 1,
                    "instant_command": {"schema_version": 1, "command": "auto_deposit_trusted_list",
                                        "command_name": "auto_deposit_trusted_list", "outcome": "completed",
                                        "reason": "LISTED", "values": deepcopy(self.list_values)}}
        result = CommandResultDTO(request_id=request_id or active.request_id,
                                  status=status, ok=status == "completed", data=data)
        return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
                                 message_id="list-result", correlation_id=active.command_message_id,
                                 session_id=session or active.session_id, timestamp_ms=1, payload=result.to_dict())

    def submit_command(self, request):
        result = super().submit_command(request)
        if self.immediate:
            message = self.terminal()
            self.graph.command_result_handler.handle(self.websocket, message)
            self.graph.command_result_handler.handle(self.websocket, message)
        return result


def setup(*, immediate=False, list_values=None):
    adapter = ListAdapter(immediate=immediate, list_values=list_values)
    route = TrustedConfirmationRouteFixture(adapter=adapter)
    tts = tts_fixture.TtsLifecycleResponseComponentIntegrationTests._tts()
    tts.text_processor = TTSTextProcessor()
    tts.prepare_input_items = MethodType(TTS.prepare_input_items, tts)
    tts.get_current_text_language_hint = lambda: "ko"
    stream = StringIO()
    logger = logging.Logger("trusted-list-delivery")
    handler = logging.StreamHandler(stream)
    handler.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(handler)
    ui = RoutedResponseUiPresentationQueue()
    outputs = []

    def output(payload):
        outputs.append(payload)
        return tts.receive_input(payload)

    publisher = RoutedExternalResponsePublisher(
        begin_generation_callback=route.pipeline.begin_response_generation,
        build_output_payload_callback=lambda text, generation: {"text": text, "response_generation": generation},
        send_output_callback=output, send_full_output_callback=lambda _: None,
        emission_capability_consumer=route.llm.trusted_user_input_ingress_claim_registry.consume_routed_response_emission_capability,
        ui_presentation_callback=ui.enqueue, log_callback=logger.info,
    )
    route.llm._get_routed_external_response_publisher = lambda: publisher
    MinecraftCommandLifecycleTerminalResponseWiring().wire(llm=publisher, extension=route.extension)
    MinecraftLifecycleTtsReceiptWiring().wire(
        llm=SimpleNamespace(log_routed_response_sink_delivery=publisher.log_sink_delivery), tts=tts)
    return SimpleNamespace(adapter=adapter, route=route, tts=tts, ui=ui,
                           stream=stream, outputs=outputs, publisher=publisher)


def drain_speech(f):
    items = []
    while not f.tts.input_queue.empty():
        items.append(f.tts.input_queue.get_nowait())
    return items


@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize("immediate", (False, True))
@pytest.mark.parametrize("count,total", ((0, 0), (1, 1), (64, 70)))
def test_chat_voice_delayed_and_coalesced_lists_preserve_ui_ids_and_filtered_speech(voice, immediate, count, total):
    f = setup(immediate=immediate, list_values=values(count, total=total))
    chat, _ = f.route.dispatch(PHRASE, voice=voice)
    assert len(f.adapter.requests) == 1
    assert f.adapter.requests[0].command == "auto_deposit_trusted_list"
    if not immediate:
        terminal = f.adapter.terminal()
        f.adapter.graph.command_result_handler.handle(f.adapter.websocket, terminal)
        f.adapter.graph.command_result_handler.handle(f.adapter.websocket, terminal)
    terminals = [p for p in f.outputs if p["response_kind"] in {"command_terminal", "command_coalesced"}]
    assert f.outputs, f.stream.getvalue()
    assert len(terminals) == 1, (f.stream.getvalue(), f.adapter.logs[-6:], chat)
    speech = terminals[0]["text"]
    assert "td_" not in speech and "검열됨" not in speech
    assert len(speech) <= 1800
    ui_messages = f.ui.snapshot()[1]
    rendered = [str(getattr(item, "content", item)) for item in (ui_messages or chat)]
    if count:
        assert any(IDENTIFIER in text for text in rendered)
        assert "1번: 오버월드 -746, 70, 66, 현재 상태 미확인." in speech
    else:
        assert "등록된 자동 보관 장소가 없어." == speech
    if count == 64:
        assert "전체 70곳 중 앞의" in speech and "나머지는 화면 목록" in speech
        assert any("td_00000000000000000000003f" in text for text in rendered)
        assert "화면에는 앞의 64곳을 표시했어." in speech
    queued = drain_speech(f)
    terminal_items = [item for item in queued if item["lifecycle_response_kind"] in {"command_terminal", "command_coalesced"}]
    assert terminal_items
    assert all("td_" not in item["text"] and "검열됨" not in item["text"] for item in terminal_items)
    assert f.adapter.graph.connection_ownership.active_command_owner is None
    assert f.route.pipeline.provider_calls == 0
    assert "event=routed_response_text_projection" in f.stream.getvalue()
    assert "filter_policy=existing_output_pipeline" in f.stream.getvalue()
    assert any(f"list_total={total} list_validated={count}" in line for line in f.adapter.logs)
    if ui_messages:
        history = RoutedResponseUiPresentationDrain(f.ui).append_to_history([])
        assert [item.content for item in history] == [item.content for item in ui_messages]


@pytest.mark.parametrize("interrupted", (False, True))
def test_queue_acceptance_is_distinct_from_complete_or_interrupted_playback(interrupted):
    f = setup()
    f.route.dispatch(PHRASE)
    drain_speech(f)
    f.adapter.graph.command_result_handler.handle(f.adapter.websocket, f.adapter.terminal())
    items = drain_speech(f)
    before = f.stream.getvalue()
    assert "sink=tts_queue" in before
    assert "sink=tts_playback" not in before
    receipt = None
    for index, item in enumerate(items):
        stop = interrupted and index == len(items) - 1
        receipt = f.tts.observe_lifecycle_response_playback(
            event_id=item["lifecycle_event_id"], route_kind=item["lifecycle_route_kind"],
            response_kind=item["lifecycle_response_kind"], delivery_token=item["lifecycle_delivery_token"],
            item_index=item["lifecycle_item_index"], played=not stop, reason="interrupted" if stop else "played")
    assert receipt is not None and receipt.observed is not interrupted
    assert ("delivered=false reason=interrupted" if interrupted else "delivered=true reason=played") in f.stream.getvalue()


@pytest.mark.parametrize("mutation", ("session", "request", "socket", "list_count", "duplicate_id", "status", "command"))
def test_foreign_or_malformed_result_cannot_generate_trusted_list_speech(mutation):
    f = setup()
    f.route.dispatch(PHRASE)
    f.outputs.clear()
    message = f.adapter.terminal()
    if mutation == "session":
        message = f.adapter.terminal(session="foreign")
    elif mutation == "request":
        message = f.adapter.terminal(request_id="foreign")
    elif mutation not in {"socket"}:
        data = deepcopy(message.payload["data"])
        native = data["instant_command"]
        if mutation == "list_count":
            native["values"]["listed"] = 2
        elif mutation == "duplicate_id":
            native["values"].update(total=2, listed=2, destinations=native["values"]["destinations"] * 2)
        elif mutation == "status":
            native["values"]["destinations"][0]["status"] = "AVAILABLE_NOW"
        else:
            native["command"] = "auto_deposit_untrust"
        message = f.adapter.terminal(data=data)
    f.adapter.graph.command_result_handler.handle(object() if mutation == "socket" else f.adapter.websocket, message)
    assert all("1번:" not in p["text"] and IDENTIFIER not in p["text"] for p in f.outputs)
    assert "event=routed_response_text_projection" not in f.stream.getvalue()


def test_fact_projection_rejects_text_labels_and_fake_owner(monkeypatch):
    assert TrustedDestinationListSpeechProjector.project(SimpleNamespace(
        verified=True, status="completed", source="minecraft_chatclef", event_id="a" * 32)) is None
    captured = []
    project = TrustedDestinationListSpeechProjector.project

    def capture(fact):
        captured.append(fact)
        return project(fact)

    monkeypatch.setattr(TrustedDestinationListSpeechProjector, "project", staticmethod(capture))
    f = setup(immediate=True)
    f.route.dispatch(PHRASE)
    fact = captured[-1]
    assert project(fact) is not None
    assert project(replace(fact, owner_token=SimpleNamespace(**vars(fact.owner_token)))) is None
    for changes in ({"command": None}, {"request_id": ""}, {"session_id": None}, {"generation": 0}):
        assert project(replace(fact, owner_token=replace(fact.owner_token, **changes))) is None
    assert project(replace(fact, event_id="f" * 32)) is None
    assert project(replace(fact, verified=False)) is None


def test_displayed_unknown_id_round_trips_to_exact_untrust_command():
    f = setup()
    f.route.dispatch(PHRASE)
    f.adapter.graph.command_result_handler.handle(f.adapter.websocket, f.adapter.terminal())
    f.route.dispatch(f"자동 보관 장소 {IDENTIFIER} 등록 해제해 줘")
    assert [request.command for request in f.adapter.requests] == [
        "auto_deposit_trusted_list", f"auto_deposit_untrust {IDENTIFIER}"]


def test_snapshot_remains_immutable_after_result_acceptance():
    f = setup()
    f.route.dispatch(PHRASE)
    message = f.adapter.terminal()
    f.adapter.graph.command_result_handler.handle(f.adapter.websocket, message)
    message.payload["data"]["instant_command"]["values"]["destinations"][0]["destination_id"] = "forged"
    assert IDENTIFIER in f.ui.snapshot()[1][-1].content
    assert "forged" not in f.outputs[-1]["text"]
