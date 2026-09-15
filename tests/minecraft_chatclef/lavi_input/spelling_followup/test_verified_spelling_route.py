#20260915_kpopmodder: Exercise verified spellings through the production Chat and final-voice graph.
from dataclasses import replace

import pytest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import ChatClefIntentSchemaValidator
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import KoreanChatClefRuleParser
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .catalogue_fixture import item_catalogue


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("raw,command", (("챗클래프 꺼줘", "chatclef off"), ("챗 클래프 켜 줘", "chatclef on")))
def test_setting_spelling_keeps_confirmation_and_original_binding(raw, command, voice):
    f = TrustedConfirmationRouteFixture()
    first, original_event = f.dispatch(raw, voice=voice)
    assert len(first) == 1 and "확인" in first[0] and f.adapter.requests == []
    if original_event is not None:
        list(f.llm.accept_queued_input(original_event, [], "system"))
        assert len(f.outputs) == 1
    _, confirmation_event = f.dispatch("확인", voice=voice)
    assert [request.command for request in f.adapter.requests] == [command]
    assert f.adapter.requests[0].metadata["natural_language"]["original_text"] == raw
    if confirmation_event is not None:
        list(f.llm.accept_queued_input(confirmation_event, [], "system"))
    f.dispatch("확인", voice=voice)
    assert len(f.adapter.requests) == 1 and f.pipeline.provider_calls == 0


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("raw", ("다이아 레겡스 장착해줘", "다이아몬드 레겡스를 장착해 줘", "다이아 레깅스 장착해줘"))
@pytest.mark.parametrize("runtime", (False, True))
def test_equipment_spelling_uses_one_native_submission(raw, runtime, voice):
    f = TrustedConfirmationRouteFixture(item_catalogue() if runtime else None)
    result = f.service.translate(raw)
    assert result.command == "equip diamond_leggings"
    response, queued = f.dispatch(raw, voice=voice)
    assert [request.command for request in f.adapter.requests] == ["equip diamond_leggings"]
    assert len(response) == len(f.outputs) == 1 and f.pipeline.provider_calls == 0
    assert "완료" not in response[0]
    if queued is not None:
        list(f.llm.accept_queued_input(queued, [], "system"))
        assert len(f.adapter.requests) == len(f.outputs) == 1


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
def test_verified_alias_cannot_bypass_native_capability(voice):
    f = TrustedConfirmationRouteFixture(item_catalogue(equip=False))
    result = f.service.translate("다이아 레겡스 장착해줘")
    assert result.reason_code == "native_command_target_unsupported" and not result.executable
    response, _ = f.dispatch("다이아 레겡스 장착해줘", voice=voice)
    assert len(response) == len(f.outputs) == 1 and "지원" in response[0]
    assert f.adapter.requests == [] and f.pipeline.provider_calls == 0


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("cancel", ("취소", "멈춰줘"))
def test_setting_spelling_cancel_and_stop_keep_existing_owner(cancel, voice):
    f = TrustedConfirmationRouteFixture()
    f.dispatch("챗클래프 꺼줘", voice=voice)
    f.dispatch(cancel, voice=voice)
    f.dispatch("확인", voice=voice)
    assert f.adapter.requests == [] and f.pipeline.provider_calls == 0
    assert len(f.adapter.stop_requests) == (1 if cancel == "멈춰줘" else 0)


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
def test_busy_equipment_stays_rejected_and_stop_is_available(voice):
    f = TrustedConfirmationRouteFixture(item_catalogue())
    f.adapter.busy = "other-operation"
    f.dispatch("다이아 레겡스 장착해줘", voice=voice)
    assert f.adapter.requests == []
    f.dispatch("멈춰줘", voice=voice)
    assert len(f.adapter.stop_requests) == 1 and f.pipeline.provider_calls == 0


def test_spelling_diagnostic_metadata_is_closed_and_original_bound():
    parser, validator = KoreanChatClefRuleParser(), ChatClefIntentSchemaValidator()
    parsed = parser.parse("챗클래프 꺼줘")
    assert validator.validate(parsed)[0]
    assert not validator.validate(replace(parsed, parse_rule_id=""))[0]
    canonical = parser.parse("챗클레프 꺼줘")
    assert not validator.validate(replace(canonical, parse_rule_id="chatclef_verified_spelling"))[0]
    equipment = parser.parse("다이아 레겡스 장착해줘")
    assert not validator.validate(replace(equipment, parse_rule_id="chatclef_verified_spelling"))[0]
    for invalid in (True, "caller_said_safe", "x\nexecute", [], None):
        assert not validator.validate({**parsed.to_dict(), "parse_rule_id": invalid})[0]


def test_optional_spelling_metadata_preserves_legacy_shape_and_round_trip():
    from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
    parser, validator = KoreanChatClefRuleParser(), ChatClefIntentSchemaValidator()
    for raw in ("챗클레프 꺼줘", "다이아 레깅스 장착해줘", "자동 보관 장소 목록 보여줘"):
        parsed = parser.parse(raw)
        payload = parsed.to_dict()
        assert "parse_rule_id" not in payload
        assert ChatClefIntentDTO.from_mapping(payload) == parsed
        assert validator.validate(payload)[0]
    selected = parser.parse("챗클래프 꺼줘")
    assert selected.to_dict()["parse_rule_id"] == "chatclef_verified_spelling"
    assert ChatClefIntentDTO.from_mapping(selected.to_dict()) == selected
    assert validator.validate(selected.to_dict())[0]
