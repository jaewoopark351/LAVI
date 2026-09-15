#20260915_kpopmodder: Ambiguous names deliver bounded Korean candidates once through real Chat/final voice ingress.
import pytest

from plugins.Minecraft.fabric.chatclef.intent.names.korean_name_resolution_message import KoreanNameResolutionMessage
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .catalogue_fixture import catalogue


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("text,kind,ids,tokens,label,reason", (
    ("벽돌 두 개 구해 줘", "item", ("brick", "bricks"), {"get": ("brick", "bricks")}, "벽돌", "후보"),
    ("벽돌 두 개와 철괴 한 개 구해 줘", "item", ("brick", "bricks"), {"get": ("brick", "bricks")}, "벽돌", "후보"),
    ("횃불 블록 위치 스캔해 줘", "block", ("torch", "wall_torch"), {"scan": ("TORCH", "WALL_TORCH")}, "횃불", "후보"),
    ("Alex에게 minecraft:coast_armor_trim_smithing_template 한 개 줘", "item",
     ("coast_armor_trim_smithing_template", "dune_armor_trim_smithing_template"),
     {"give": ("smithing_template", "smithing_template")}, "대장장이 형판", "기존 명령"),
))
def test_ambiguity_candidates_reach_screen_and_speech_without_submission(text, kind, ids, tokens, label, reason, voice):
    snapshot = catalogue()
    for index, identifier in enumerate(ids):
        snapshot["entries"].append({
            "kind": kind, "id": "minecraft:" + identifier,
            "translation_key": kind + ".minecraft." + identifier, "korean_name": label,
            "tokens": {command: values[index] for command, values in tokens.items()},
            "capabilities": list(tokens),
        })
    f = TrustedConfirmationRouteFixture(snapshot)
    speech = []
    f.llm.event_dispatcher.add_output_event_listener(speech.append)
    response, _ = f.dispatch(text, voice=voice)
    assert len(response) == len(f.outputs) == len(speech) == 1
    assert f.outputs == speech
    message = speech[0]["text"]
    assert reason in message and all("minecraft:" + identifier in message for identifier in ids)
    assert "말해" in message or "선택" in message
    assert f.adapter.requests == [] and f.adapter.stop_requests == []
    assert f.pipeline.provider_calls == 0


@pytest.mark.parametrize("suggestions", (("minecraft:brick\nexecute",), ("minecraft:brick",) * 2, ("x" * 257,)))
def test_candidate_renderer_never_echoes_malformed_or_repeated_ids(suggestions):
    assert KoreanNameResolutionMessage.ambiguous({
        "status": "ambiguous", "reason_code": "ambiguous_registered_name", "data": {"suggestions": suggestions},
    }) is None


def test_runtime_candidate_reply_requires_live_proof_and_exact_original_intent():
    from copy import deepcopy
    from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.runtime_name_ambiguity_rejection import RuntimeNameAmbiguityRejection
    snapshot = catalogue()
    snapshot["entries"].extend([
        {"kind": "item", "id": "minecraft:" + identifier, "translation_key": "item.minecraft." + identifier,
         "korean_name": "벽돌", "tokens": {"get": identifier}, "capabilities": ["get"]}
        for identifier in ("brick", "bricks")
    ])
    f = TrustedConfirmationRouteFixture(snapshot)
    raw = "벽돌 두 개 구해 줘"
    translation = f.service.translate(raw).to_dict()
    assert RuntimeNameAmbiguityRejection.message(translation, raw, True)
    assert RuntimeNameAmbiguityRejection.message(translation, raw, False) is None
    assert RuntimeNameAmbiguityRejection.message(translation, "철괴 두 개 구해 줘", True) is None
    bad = deepcopy(translation)
    bad["data"].pop("runtime_catalogue")
    assert RuntimeNameAmbiguityRejection.message(bad, raw, True) is None


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("native_collision", (False, True), ids=("name", "native_token"))
def test_actual_ambiguity_reason_file_sink_and_throwing_sink_preserve_response(tmp_path, voice, native_collision):
    import logging
    snapshot = catalogue()
    ids = ("brick", "bricks")
    command = "give" if native_collision else "get"
    text = "Alex에게 minecraft:brick 두 개 줘" if native_collision else "벽돌 두 개 구해 줘"
    reason = "ambiguous_native_command_token" if native_collision else "ambiguous_registered_name"
    snapshot["entries"].extend([
        {"kind": "item", "id": "minecraft:" + identifier, "translation_key": "item.minecraft." + identifier,
         "korean_name": "벽돌", "tokens": {command: "brick" if native_collision else identifier}, "capabilities": [command]}
        for identifier in ids
    ])
    path = tmp_path / "ambiguity.log"
    logger = logging.Logger("korean-name-ambiguity")
    handler = logging.FileHandler(path, encoding="utf-8")
    logger.addHandler(handler)
    f = TrustedConfirmationRouteFixture(snapshot)
    f.llm.input_router._feature_admission_logger._callback = logger.info
    response, _ = f.dispatch(text, voice=voice)
    handler.close()
    output = path.read_text(encoding="utf-8")
    assert output.count("event=minecraft_korean_feature_admission") == 1
    assert "decision=handled" in output and "reason=" + reason in output
    assert text not in output and "minecraft:brick" not in output

    def fail(_):
        raise OSError("sink unavailable")

    broken = TrustedConfirmationRouteFixture(snapshot)
    broken.llm.input_router._feature_admission_logger._callback = fail
    actual, _ = broken.dispatch(text, voice=voice)
    assert actual == response
    assert len(broken.outputs) == len(f.outputs) == 1
    assert broken.adapter.requests == f.adapter.requests == []
    assert broken.pipeline.provider_calls == f.pipeline.provider_calls == 0
