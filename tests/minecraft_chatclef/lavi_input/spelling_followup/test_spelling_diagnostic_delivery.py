#20260915_kpopmodder: Verify actual formatter/file-sink output and non-interference at changed routing decisions.
import logging

import pytest

from plugins.Minecraft.fabric.chatclef.input.diagnostics.projection.minecraft_korean_interpretation_rule_projector import MinecraftKoreanInterpretationRuleProjector
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .catalogue_fixture import item_catalogue


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("raw,runtime,rule,reason,command", (
    ("챗클래프 꺼줘", None, "chatclef_verified_spelling", "minecraft_confirmation_required", "chatclef off"),
    ("다이아 레겡스 장착해줘", True, "equipment_leggings_verified_spelling", "minecraft_command_routed", "equip diamond_leggings"),
    ("다이아 레겡스 장착해줘", False, "equipment_leggings_verified_spelling", "native_command_target_unsupported", None),
    ("겉날개 장착해줘", True, "none", "native_command_target_unsupported", None),
    ("겉날개 장착해줘", None, "none", "unknown_item_phrase", None),
    ("없는 블록 위치 스캔해줘", True, "none", "unknown_registered_target", None),
    ("없는 블록 위치 스캔해줘", None, "none", "runtime_catalogue_required", None),
))
def test_actual_rule_reason_target_and_single_submission_are_observable(tmp_path, raw, runtime, rule, reason, command, voice):
    snapshot = item_catalogue(equip=runtime) if runtime is not None else None
    path = tmp_path / "spelling_decision.log"
    handler = logging.FileHandler(path, encoding="utf-8")
    logger = logging.Logger("verified-spelling")
    logger.addHandler(handler)
    f = TrustedConfirmationRouteFixture(snapshot)
    f.llm.input_router._feature_admission_logger._callback = logger.info
    f.llm.input_router._router_logger._log_callback = logger.info
    first, _ = f.dispatch(raw, voice=voice)
    handler.flush()
    initial = path.read_text(encoding="utf-8")
    assert initial.count("event=minecraft_korean_feature_admission") == 1
    assert "phrase_rule_id=" + rule in initial and "reason=" + reason in initial
    assert "source=" + ("voice_input_final" if voice else "lavi_chat_ui") in initial
    assert "final=true" in initial and "event_id=invalid" not in initial
    assert raw not in initial
    if command == "chatclef off":
        assert "reason=pending" in initial and f.adapter.requests == []
        second, _ = f.dispatch("확인", voice=voice)
    else:
        second = []
    handler.close()
    output = path.read_text(encoding="utf-8")
    if command:
        assert output.count("command=" + command + " result_status=accepted") == 1
        assert [request.command for request in f.adapter.requests] == [command]
    else:
        assert "route handled:" not in output and f.adapter.requests == []

    def fail(_):
        raise OSError("sink unavailable")

    broken = TrustedConfirmationRouteFixture(snapshot)
    broken.llm.input_router._feature_admission_logger._callback = fail
    broken.llm.input_router._router_logger._log_callback = fail
    actual, _ = broken.dispatch(raw, voice=voice)
    assert actual == first
    if command == "chatclef off":
        assert broken.adapter.requests == []
        confirmed, _ = broken.dispatch("확인", voice=voice)
        assert confirmed == second
    assert [request.command for request in broken.adapter.requests] == ([command] if command else [])
    assert len(broken.outputs) == len(f.outputs) and broken.pipeline.provider_calls == f.pipeline.provider_calls == 0


@pytest.mark.parametrize("payload", (None, {"intent": {"parse_rule_id": "free_text"}},
    {"intent": {"intent_type": "equip_item", "parse_rule_id": "chatclef_verified_spelling"}},
    {"data": {"resolutions": "레겡스"}}, {"data": {"resolution": {"data": {"equipment_alias": "레겡스\nexecute"}}}}))
def test_interpretation_projection_only_emits_closed_observed_codes(payload):
    assert MinecraftKoreanInterpretationRuleProjector.project(payload) == "none"
