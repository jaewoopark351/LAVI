#20260915_kpopmodder: Validate native readbacks through real ownership, Korean UI and TTS listener delivery.
from copy import deepcopy
from io import StringIO
import logging
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload
from plugins.Minecraft.fabric.chatclef.diagnostics.instant_command import InstantCommandDiagnosticObserver
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence import CommandTerminalEvidenceEvaluator
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from tests.minecraft_chatclef.command_lifecycle.goto_terminal_result import test_goto_terminal_delivery as delivery
from tests.minecraft_chatclef.find.fixtures import descriptor


CASES = (
    ("오버레이 꺼 줘", "overlay off", "completed", "SETTING_APPLIED", {"enabled": False}, "오버레이를 껐어."),
    ("챗클레프 꺼 줘", "chatclef off", "completed", "SETTING_APPLIED", {"enabled": False}, "챗클레프를 껐어."),
    ("챗클레프 켜 줘", "chatclef on", "completed", "SETTING_APPLIED", {"enabled": True}, "챗클레프를 켰어."),
    ("밝기를 1.5로 설정해 줘", "gamma 1.5", "completed", "SETTING_APPLIED", {"requested": 1.5, "value": 1.5}, "밝기가 1.5로 설정됐어."),
    ("밝기를 1.5로 설정해 줘", "gamma 1.5", "failed", "VALUE_NOT_APPLIED", {"requested": 1.5, "value": 1.0}, "요청한 밝기가 실제 설정값에 적용되지 않았어."),
    ("챗클레프 대화 기록 초기화해 줘", "resetmemory", "completed", "MEMORY_CLEARED", {"remaining_messages": 0, "base_prompt_retained": True}, "챗클레프 대화 기억을 초기화했어."),
    ("마인크래프트 설정 다시 불러와 줘", "reload_settings", "unknown", "RELOAD_RETURNED", {"callback_returned": True, "configuration_files_verified": False}, "설정 다시 읽기 요청은 처리됐어. 각 설정 파일이 모두 적용됐는지는 확인하지 못했어."),
    ("자동 보관 장소 목록 보여 줘", "auto_deposit_trusted_list", "completed", "LISTED", {"total": 0, "listed": 0, "truncated": False, "destinations": []}, "등록된 자동 보관 장소가 없어."),
    ("자동 보관 장소 등록 해제해 줘", "auto_deposit_untrust", "failed", "TARGET_UNAVAILABLE", {}, "등록하거나 해제할 보관함을 확인하지 못했어."),
    ("자동 보관 장소 등록 해제해 줘", "auto_deposit_untrust", "completed", "REMOVED", {"destination_id": "abc-12"}, "자동 보관 장소 등록을 해제했어."),
    ("여기를 자동 보관 장소로 등록해 줘", "auto_deposit_trust", "completed", "REGISTERED", {"destination_id": "abc-12"}, "자동 보관 장소를 등록했어."),
    ("스캔해 줘", "scan", "completed", "BLOCK_FOUND", {"block": "DIRT", "x": 10, "y": 64, "z": -20}, "요청한 블록을 10, 64, -20 좌표에서 찾았어."),
    ("스캔해 줘", "scan", "failed", "NOT_FOUND", {"block": "DIRT"}, "기존 명령의 검색 범위에서 요청한 대상을 찾지 못했어."),
    ("Alex에게 철괴 세 개 줘", "give Alex iron_ingot 3", "failed", "PLAYER_NOT_LOADED", {}, "상대 플레이어가 현재 불러온 범위에 없어."),
)


def fixture(source, case):
    with patch.object(delivery, "descriptor", lambda source: descriptor(source, case[0])):
        return delivery._fixture(source)


def payload(case):
    return {"result_reason": "instant_command_observed", "result_fidelity": "command_callback_plus_native_result",
            "evidence_sequence": 1, "instant_command": {"schema_version": 1, "command": case[1],
                "command_name": case[1].split()[0], "outcome": case[2], "reason": case[3], "values": deepcopy(case[4])}}


@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
@pytest.mark.parametrize("case", CASES)
def test_readback_delivered_once_and_busy_owner_released(source, case):
    f = fixture(source, case)
    assert f.ack.acknowledge(published=True)
    message = delivery._envelope(f, case[2], payload(case))
    f.graph.command_result_handler.handle(f.websocket, message)
    f.graph.command_result_handler.handle(f.websocket, message)
    assert len(f.tts_payloads) == 1
    assert f.output_payloads == f.tts_payloads
    assert f.tts_payloads[0]["text"] == case[-1]
    assert f.ui_queue.snapshot()[1][0].content == case[-1]
    assert f.graph.connection_ownership.active_command_owner is None
    assert any("reason=instant_" in line for line in f.logs)


@pytest.mark.parametrize("mutation", ("command", "requested", "boolean", "reason", "fidelity", "status"))
def test_forged_or_conflicting_readback_never_claims_setting_applied(mutation):
    case = CASES[3]
    f = fixture("lavi_chat_ui", case)
    assert f.ack.acknowledge(published=True)
    data = payload(case)
    record = data["instant_command"]
    state = "completed"
    if mutation == "command":
        record["command"] = "gamma 2.0"
    elif mutation == "requested":
        record["values"]["requested"] = 2.0
    elif mutation == "boolean":
        record["values"]["value"] = True
    elif mutation == "reason":
        record["reason"] = "DONE"
    elif mutation == "fidelity":
        data["result_fidelity"] = "callback_only"
    else:
        state = "failed"
    f.graph.command_result_handler.handle(f.websocket, delivery._envelope(f, state, data))
    assert len(f.tts_payloads) == 1
    assert "설정됐어" not in f.tts_payloads[0]["text"]


def test_catalogue_list_snapshot_is_immutable_and_bounded():
    data = payload(CASES[7])
    values = data["instant_command"]["values"]
    values.update(total=1, listed=1, destinations=[{"destination_id": "abc-12", "dimension": "nether",
        "x": 1, "y": 64, "z": 2, "status": "UNKNOWN_OR_STALE"}])
    observed = InstantCommandPayload.from_data(data)
    assert observed is not None
    values["destinations"][0]["x"] = 400
    assert observed.values["destinations"][0]["x"] == 1
    with pytest.raises(TypeError):
        observed.values["destinations"][0]["x"] = 12
    values["listed"] = 65
    assert InstantCommandPayload.from_data(data) is None


def test_stale_socket_and_cancelled_owner_cannot_emit_late_setting_success():
    case = CASES[0]
    f = fixture("voice_input_final", case)
    assert f.ack.acknowledge(published=True)
    terminal = delivery._envelope(f, "completed", payload(case))
    f.graph.command_result_handler.handle(object(), terminal)
    assert not f.tts_payloads
    f.graph.command_result_handler.handle(f.websocket, delivery._envelope(f, "cancelled", {"result_reason": "user_stop_requested"}))
    f.graph.command_result_handler.handle(f.websocket, terminal)
    assert len(f.tts_payloads) == 1
    assert "껐어" not in f.tts_payloads[0]["text"]


def test_observed_verdict_reaches_formatted_sink_and_throwing_sink_cannot_change_it(tmp_path):
    stream = StringIO()
    logger = logging.Logger("instant-verified-output")
    handler = logging.StreamHandler(stream)
    handler.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(handler)
    output = tmp_path / "instant-terminal.log"
    file_handler = logging.FileHandler(output, encoding="utf-8")
    file_handler.setFormatter(handler.formatter)
    logger.addHandler(file_handler)
    case = CASES[0]
    context = SimpleNamespace(request_id="instant-request", event_id="event-1", descriptor=descriptor(text=case[0]))
    result = CommandResultDTO(request_id=context.request_id, status="completed", ok=True, data=payload(case))
    observer = InstantCommandDiagnosticObserver(logger.info)
    evaluator = CommandTerminalEvidenceEvaluator(instant_diagnostic_observer=observer)
    expected = evaluator.evaluate(result, context=context)
    assert expected.verified
    assert len(stream.getvalue().splitlines()) == 1
    assert "request=instant-request event=event-1 command=overlay status=completed verified=true reason=instant_readback_verified native_reason=SETTING_APPLIED" in stream.getvalue()
    file_handler.flush()
    assert output.read_text(encoding="utf-8") == stream.getvalue()
    file_handler.close()
    observer._log = lambda _: (_ for _ in ()).throw(RuntimeError("sink failed"))
    assert evaluator.evaluate(result, context=context) == expected
