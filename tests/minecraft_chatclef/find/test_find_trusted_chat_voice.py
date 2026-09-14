#20260914_kpopmodder: Use real trusted ingress receipts; the adapter records requests instead of Minecraft execution.
import pytest
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.extension import MinecraftFabricChatClefExtension
from tests.minecraft_chatclef.lavi_input.test_trusted_korean_chat_voice_integration import (
    _RecordingMinecraftAdapter, _llm_harness, _dispatch_goto_input,
)
from .test_find_request_and_input import CASES

# Preserve the existing Korean-only automatic admission boundary. English DSL remains native/raw-GUI input.
KOREAN_CASES = tuple((text, cmd) for text, cmd in CASES if any("가" <= c <= "힣" for c in text)) + (
    ("엔티티 minecraft:villager 찾아줘", "find entity minecraft:villager approach"),
    ("떨어진 example:rare_gem 찾아줘", "find item example:rare_gem approach"),
)

@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize("text, expected", KOREAN_CASES)
def test_chat_and_final_voice_submit_exactly_once_without_llm(text, expected, voice):
    adapter = _RecordingMinecraftAdapter()
    service = ChatClefNaturalLanguageService()
    extension = MinecraftFabricChatClefExtension(adapter=adapter, natural_language_service=service)
    llm, pipeline, _ = _llm_harness(extension)
    _, queued = _dispatch_goto_input(llm, text, voice=voice)
    assert len(adapter.requests) == 1
    assert adapter.requests[0].command == expected
    assert adapter.requests[0].source == ("voice_input_final" if voice else "lavi_chat_ui")
    assert adapter.requests[0].metadata["natural_language"]["original_text"] == text
    assert pipeline.provider_calls == 0
    if queued is not None:
        list(llm.accept_queued_input(queued, [], "system"))
        assert len(adapter.requests) == 1, "Replayed final-voice receipt must not submit twice"

@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize("text", ("좀비 찾지 마", "상자 찾아줘 그리고 부숴줘", "@find item diamond;@attack zombie", "주민 찾아줄 수 있어?"))
def test_guarded_find_never_submits(text, voice):
    adapter = _RecordingMinecraftAdapter()
    extension = MinecraftFabricChatClefExtension(adapter=adapter, natural_language_service=ChatClefNaturalLanguageService())
    llm, pipeline, _ = _llm_harness(extension)
    _dispatch_goto_input(llm, text, voice=voice)
    assert adapter.requests == []

@pytest.mark.parametrize("voice", (False, True))
def test_english_only_dsl_does_not_bypass_korean_automatic_admission(voice):
    adapter = _RecordingMinecraftAdapter()
    extension = MinecraftFabricChatClefExtension(adapter=adapter, natural_language_service=ChatClefNaturalLanguageService())
    llm, _, _ = _llm_harness(extension)
    _dispatch_goto_input(llm, "@find entity minecraft:villager", voice=voice)
    assert adapter.requests == []
