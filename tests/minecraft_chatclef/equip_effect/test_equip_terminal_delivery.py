#20260915_kpopmodder: Exercise real server correlation, once-only terminal delivery, cancellation and formatted observations.
from io import StringIO
import logging
from types import SimpleNamespace
from unittest.mock import patch
import pytest

from plugins.Minecraft.fabric.chatclef.diagnostics.equip_command import EquipCommandDiagnosticObserver
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence import CommandTerminalEvidenceEvaluator
from tests.minecraft_chatclef.command_lifecycle.goto_terminal_result import test_goto_terminal_delivery as delivery
from tests.minecraft_chatclef.find.fixtures import descriptor
from .fixtures import context, data, result


def fixture(source, text="다이아 바지 장착해 줘"):
    desc = descriptor(source, text)
    with patch.object(delivery, "descriptor", lambda source: desc):
        value = delivery._fixture(source)
    value.equip_descriptor = desc
    return value


def wire(f, outcome, *, targets=None):
    ctx = SimpleNamespace(request_id=f.active.request_id, session_id=f.active.session_id,
                          generation=f.active.generation, descriptor=f.equip_descriptor)
    return data(ctx, outcome, targets=targets)


@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
@pytest.mark.parametrize("outcome,phrase", (("satisfied", "착용을 확인했어"),
                                          ("already_satisfied", "이미 착용하고 있어"),
                                          ("not_satisfied", "착용돼 있지 않아")))
def test_terminal_effect_delivered_once_and_owner_released(source, outcome, phrase):
    f = fixture(source)
    assert f.ack.acknowledge(published=True)
    message = delivery._envelope(f, "completed", wire(f, outcome))
    f.graph.command_result_handler.handle(f.websocket, message)
    f.graph.command_result_handler.handle(f.websocket, message)
    assert len(f.tts_payloads) == 1 and phrase in f.tts_payloads[0]["text"]
    assert f.output_payloads == f.tts_payloads
    assert f.ui_queue.snapshot()[1][0].content == f.tts_payloads[0]["text"]
    assert f.graph.connection_ownership.active_command_owner is None
    assert sum("equip_command_evidence " in line for line in f.logs) == 1


def test_legacy_payload_keeps_cautious_feedback():
    f = fixture("lavi_chat_ui")
    assert f.ack.acknowledge(published=True)
    f.graph.command_result_handler.handle(f.websocket, delivery._envelope(f, "completed", {
        "result_reason": "matching_task_finished", "result_fidelity": "callback_plus_matching_user_task_event"}))
    assert "실제로 장착됐는지는 확인하지 못했어" in f.tts_payloads[0]["text"]


@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
@pytest.mark.parametrize("outcome,phrase", (("satisfied", "요청한 장비 착용을 확인했어."),
                                          ("partial", "2개 대상 중 1개 대상만")))
def test_korean_target_list_requires_every_target_for_both_input_sources(source, outcome, phrase):
    f = fixture(source, "다이아 투구 한 개와 다이아 바지 한 개 장착해 줘")
    assert f.equip_descriptor.command == "equip [diamond_helmet 1, diamond_leggings 1]"
    targets = [{"index": index, "native_target": native, "requested_count": 1,
                "matches": [{"item_id": "minecraft:" + native, "slot": slot}]}
               for index, (native, slot) in enumerate((("diamond_leggings", "legs"),
                                                        ("diamond_helmet", "head")))]
    assert f.ack.acknowledge(published=True)
    terminal = delivery._envelope(f, "completed", wire(f, outcome, targets=targets))
    f.graph.command_result_handler.handle(f.websocket, terminal)
    f.graph.command_result_handler.handle(f.websocket, terminal)
    assert len(f.tts_payloads) == 1 and phrase in f.tts_payloads[0]["text"]
    assert f.ui_queue.snapshot()[1][0].content == f.tts_payloads[0]["text"]
    assert f.graph.connection_ownership.active_command_owner is None


def test_wrong_socket_and_cancelled_owner_never_accept_late_slot_success():
    f = fixture("voice_input_final")
    assert f.ack.acknowledge(published=True)
    message = delivery._envelope(f, "completed", wire(f, "satisfied"))
    f.graph.command_result_handler.handle(object(), message)
    assert not f.tts_payloads
    f.graph.command_result_handler.handle(f.websocket, delivery._envelope(f, "cancelled", {"result_reason": "user_stop_requested"}))
    f.graph.command_result_handler.handle(f.websocket, message)
    assert len(f.tts_payloads) == 1 and "중단" in f.tts_payloads[0]["text"]


def test_actual_formatted_output_and_sink_failure_do_not_change_effect(tmp_path):
    stream = StringIO()
    logger = logging.Logger("equip-slot-proof")
    console = logging.StreamHandler(stream)
    console.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(console)
    file = tmp_path / "equip-evidence.log"
    sink = logging.FileHandler(file, encoding="utf-8")
    sink.setFormatter(console.formatter)
    logger.addHandler(sink)
    ctx = context()
    observer = EquipCommandDiagnosticObserver(logger.info)
    evaluator = CommandTerminalEvidenceEvaluator(equip_diagnostic_observer=observer)
    expected = evaluator.evaluate(result(ctx, data(ctx)), context=ctx)
    assert expected.verified
    sink.flush()
    assert file.read_text(encoding="utf-8") == stream.getvalue()
    assert "verified=true reason=equip_satisfied observed=satisfied targets=1 satisfied=1" in stream.getvalue()
    sink.close()
    observer._log = lambda _: (_ for _ in ()).throw(RuntimeError("sink unavailable"))
    assert evaluator.evaluate(result(ctx, data(ctx)), context=ctx) == expected
