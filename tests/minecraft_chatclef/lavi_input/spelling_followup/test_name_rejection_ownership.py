#20260915_kpopmodder: Failures stay with Minecraft only when trusted deterministic request and name evidence agree.
from copy import deepcopy

import pytest

from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.name.minecraft_name_rejection_policy import MinecraftNameRejectionPolicy
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .catalogue_fixture import item_catalogue


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("raw,runtime,reason", (
    ("겉날개 장착해줘", False, "unknown_item_phrase"),
    ("겉날개 장착해줘", True, "native_command_target_unsupported"),
    ("없는 블록 위치 스캔해줘", False, "runtime_catalogue_required"),
    ("없는 블록 위치 스캔해줘", True, "unknown_registered_target"),
    ("마크 미등록물품 구해줘", True, "unknown_registered_item"),
    ("minecraft:missing_item 장착해줘", True, "unknown_registered_item"),
    ("겉날개 한 개와 철괴 한 개 구해줘", False, "unknown_item_phrase"),
))
def test_name_failure_is_replied_once_without_llm_or_submission(raw, runtime, reason, voice):
    f = TrustedConfirmationRouteFixture(item_catalogue() if runtime else None)
    assert f.service.translate(raw).reason_code == reason
    speech = []
    f.llm.event_dispatcher.add_output_event_listener(speech.append)
    response, queued = f.dispatch(raw, voice=voice)
    assert len(response) == len(f.outputs) == len(speech) == 1
    assert f.outputs == speech and "[Minecraft]" in response[0]
    assert f.adapter.requests == [] and f.pipeline.provider_calls == 0
    if queued is not None:
        list(f.llm.accept_queued_input(queued, [], "system"))
        assert len(f.outputs) == 1 and f.adapter.requests == []


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("raw", (
    "노트북에 새 부품 장착해줘", "문서 스캔해줘", "새 원피스 구해줘", "챗클랲 꺼줘",
    "챗클래프 꺼줘?", "챗클래프 끄지 마", "다이아 레겡스 장착해줘?",
    "다이아 레겡스 장착하지 마", "‘챗클래프 꺼줘’라고 말했어", "'다이아 레겡스 장착해줘'",
))
def test_ordinary_or_guarded_utterances_never_execute(raw, voice):
    f = TrustedConfirmationRouteFixture(item_catalogue())
    f.dispatch(raw, voice=voice)
    assert f.adapter.requests == f.adapter.stop_requests == []
    # General conversation remains available; safety guards may instead own the refusal.
    if raw in {"노트북에 새 부품 장착해줘", "문서 스캔해줘", "새 원피스 구해줘", "챗클랲 꺼줘"}:
        assert f.pipeline.provider_calls == 1
    f.dispatch("확인", voice=voice)
    assert f.adapter.requests == []


@pytest.mark.parametrize("mutation", ("untrusted", "different_text", "status_type", "unknown_reason", "reason_type", "injected_command", "different_intent", "forbidden_slot", "wrong_resolution", "no_catalogue"))
def test_rejection_policy_never_trusts_unbound_or_open_payload(mutation):
    f = TrustedConfirmationRouteFixture(item_catalogue())
    raw, live = "겉날개 장착해줘", True
    translation = deepcopy(f.service.translate(raw).to_dict())
    if mutation == "untrusted":
        live = False
    if mutation == "different_text":
        raw = "새 옷 장착해줘"
    if mutation == "status_type":
        translation["status"] = []
    if mutation == "unknown_reason":
        translation["reason_code"] = "arbitrary_user_message"
    if mutation == "reason_type":
        translation["reason_code"] = []
    if mutation == "injected_command":
        translation["command"] = "gamer"
    if mutation == "different_intent":
        translation["intent"]["item_phrase"] = "철괴"
    if mutation == "forbidden_slot":
        translation["intent"]["command"] = "equip diamond_leggings"
    if mutation == "wrong_resolution":
        translation["data"]["resolution"]["reason_code"] = "unknown_item_phrase"
    if mutation == "no_catalogue":
        translation["data"].pop("runtime_catalogue")
    assert MinecraftNameRejectionPolicy().message(translation, raw, live) is None
