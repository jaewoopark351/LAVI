#20260914_kpopmodder: Correlated FIND outcomes use production lifecycle delivery to both UI and TTS listeners.
from copy import deepcopy
from unittest.mock import patch
import pytest
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from tests.minecraft_chatclef.command_lifecycle.goto_terminal_result import test_goto_terminal_delivery as delivery
from .fixtures import descriptor, terminal_data


def fixture(source):
    # The transport harness is generic; replace only the command descriptor, not its production graph.
    with patch.object(delivery, "descriptor", descriptor):
        return delivery._fixture(source)


def envelope(f, code="ARRIVED", status=None, data=None):
    status = status or ("completed" if code in {"ARRIVED", "FOUND"} else "failed")
    payload = terminal_data(code) if data is None else data
    payload["evidence_sequence"] = 2
    result = CommandResultDTO(request_id=f.active.request_id, status=status, ok=status=="completed",
                              error_code="internal_error" if status=="failed" else None, data=payload)
    return BridgeEnvelopeDTO(protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
                             message_id="result-find", correlation_id=f.active.command_message_id,
                             session_id=f.active.session_id, timestamp_ms=1, payload=result.to_dict())

@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
@pytest.mark.parametrize("code, expected", (
    ("ARRIVED", "주민 근처에 도착했어. 대상 좌표 10, 64, -20."),
    ("NOT_FOUND", "현재 로드된 주변 반경 64블록 범위에서 대상 ‘주민’을 찾지 못했어."),
    ("TARGET_LOST", "찾았던 대상이 사라지거나 탐색 범위를 벗어났어. 도착으로 처리하지 않았어."),
    ("NO_APPROACH", "대상은 찾았지만 가까이 설 수 있는 안전한 위치를 찾지 못했어."),
    ("APPROACH_TIMEOUT", "대상은 찾았지만 제한 시간 안에 접근하지 못했어."),
    ("SEARCH_LIMIT", "탐색 제한에 도달했어. 확인하지 못한 범위까지 없다고 판단하지 않았어."),
    ("SCOPE_CHANGED", "월드나 차원이 바뀌어서 찾기를 종료했어."),
    ("PLAYER_UNAVAILABLE", "플레이어 상태를 확인할 수 없어서 찾기를 종료했어."),
    ("INTERNAL_ERROR", "찾기 처리 중 오류가 생겨서 종료했어. 로그를 확인해 줘."),
))
def test_terminal_is_delivered_once_to_identical_ui_and_tts_text(source, code, expected):
    f=fixture(source); assert f.ack.acknowledge(published=True)
    message=envelope(f, code)
    f.graph.command_result_handler.handle(f.websocket, message)
    f.graph.command_result_handler.handle(f.websocket, message)
    assert len(f.tts_payloads)==1
    assert f.output_payloads == f.tts_payloads
    entries=f.ui_queue.snapshot()[1]
    assert len(entries)==1
    assert entries[0].content == f.tts_payloads[0]["text"]
    text=f.tts_payloads[0]["text"]
    assert text == expected  # Exact verified reason, not merely absence of a success sentence.
    assert f.generations == []


def test_terminal_waits_for_start_ack_then_delivers_once():
    f=fixture("voice_input_final")
    f.graph.command_result_handler.handle(f.websocket, envelope(f))
    assert not f.tts_payloads
    assert f.ack.acknowledge(published=True)
    assert len(f.tts_payloads)==1


def test_foreign_socket_cannot_publish_arrival():
    f=fixture("lavi_chat_ui"); f.ack.acknowledge(published=True)
    f.graph.command_result_handler.handle(object(), envelope(f))
    assert not f.tts_payloads
    f.graph.command_result_handler.handle(f.websocket, envelope(f))
    assert len(f.tts_payloads)==1


def test_cancellation_wins_over_late_success():
    f=fixture("lavi_chat_ui"); f.ack.acknowledge(published=True)
    f.graph.command_result_handler.handle(f.websocket, envelope(f, status="cancelled", data={"result_reason":"user_stop_requested"}))
    f.graph.command_result_handler.handle(f.websocket, envelope(f))
    assert len(f.tts_payloads)==1
    assert "중단" in f.tts_payloads[0]["text"]
    assert "도착했어" not in f.tts_payloads[0]["text"]


@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
@pytest.mark.parametrize("mutation", ("wrong_query", "missing_boolean", "fake_success"))
def test_unverified_failed_find_cannot_publish_a_specific_reason(source, mutation):
    f = fixture(source)
    assert f.ack.acknowledge(published=True)
    data = terminal_data("NOT_FOUND")
    if mutation == "wrong_query":
        data["find"]["query"] = "좀비"
    elif mutation == "missing_boolean":
        data["find"].pop("scan_complete")
    else:
        data["find"]["observed"] = True
    f.graph.command_result_handler.handle(f.websocket, envelope(f, code="NOT_FOUND", data=data))
    assert len(f.tts_payloads) == 1
    assert f.tts_payloads[0]["text"] == "대상 찾기 작업에 실패했어"
    assert f.output_payloads == f.tts_payloads
