#20260914_kpopmodder: Korean/raw grammar, hostile input, and original trusted utterance binding.
from copy import deepcopy
from types import SimpleNamespace
import pytest
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.navigation.find import FindRequest, KoreanFindRuleParser
from plugins.Minecraft.fabric.chatclef.input.routing.find.find_translation_binding_stage import FindTranslationBindingStage

CASES = (
    ("마을 주민 찾아줘", "find auto 마을 주민 approach"),
    ("마을주민 찾아줘", "find auto 마을주민 approach"),
    ("좀비 찾아줘", "find auto 좀비 approach"),
    ("상자 블록 찾아줘", "find block 상자 블록 approach"),
    ("상자 찾아줘", "find auto 상자 approach"),
    ("떨어진 다이아몬드 찾아줘", "find item 다이아몬드 approach"),
    ("다이아몬드 아이템을 찾아줘", "find item 다이아몬드 approach"),
    ("다이아몬드 블록 찾아줘", "find block 다이아몬드 블록 approach"),
    ("라비야, 철 골렘을 좀 찾아 주세요.", "find auto 철 골렘 approach"),
    ("플레이어 Steve 찾아줘", "find player Steve approach"),
    ("모드몹 위치만 알려줘", "find auto 모드몹 report"),
    ("주민 찾아서 알려줘", "find auto 주민 report"),
    ("주민 찾아서 가줘", "find auto 주민 approach"),
    ("@find entity minecraft:villager", "find entity minecraft:villager approach"),
    ("@find item example:rare_gem report", "find item example:rare_gem report"),
    ("find block 상자 report", "find block 상자 report"),
)

@pytest.mark.parametrize("text, command", CASES)
def test_whole_request_is_deterministically_compiled(text, command):
    translation = ChatClefNaturalLanguageService().translate(text)
    assert translation.executable, translation.to_dict()
    assert translation.command == command
    assert FindRequest.parse(command).compile() == command

@pytest.mark.parametrize("text", (
    "좀비 찾지 마", "좀비 찾아줄 수 있어?", "좀비 안 찾아줘", "좀비 찾아줘 그리고 죽여줘",
    "상자 찾아줘; stop", "상자 찾아줘\n@attack zombie", "@find", "@find item", "@find report",
    "@find item diamond;@attack zombie", "@find entity zombie # report", "@find entity 좀\u200b비",
    "@find player 주민", "@find player ThisNameIsLongerThan16", "@find entity @attack",
    "다이아몬드 찾아줘 말고 보관해줘", "좀비 찾아줘?", "상자 위치만 알려줘?",
))
def test_unsafe_or_nonimperative_find_never_executes(text):
    result = ChatClefNaturalLanguageService().translate(text)
    assert not result.executable, result.to_dict()
    assert result.command is None

@pytest.mark.parametrize("command", (
    "find", "@find item", "@find item report", "find foo;bar", "find foo#bar", 'find "foo"',
    "find foo\tbar", "find foo\nbar", "find foo\u200bbar", "find foo @stop", "@@find foo",
    "find player -", "find " + "a" * 129,
))
def test_strict_raw_parser_rejects_unsafe_commands(command):
    with pytest.raises(ValueError): FindRequest.parse(command)

@pytest.mark.parametrize("command, expected", (
    ("@FIND ENTITY minecraft:zombie REPORT", FindRequest("entity", "minecraft:zombie", "report")),
    ("find 상자", FindRequest("auto", "상자")),
    ("find entity Iron Golem", FindRequest("entity", "Iron Golem")),
    ("find item mod:item_path/sub", FindRequest("item", "mod:item_path/sub")),
    ("find player _Steve1", FindRequest("player", "_Steve1")),
))
def test_raw_parser_round_trip(command, expected):
    assert FindRequest.parse(command) == expected
    assert FindRequest.parse(expected.compile()) == expected

@pytest.mark.parametrize("mutate", (
    lambda t: t.update(command="find auto 좀비 approach"),
    lambda t: t["intent"].update(source="llm"),
    lambda t: t["intent"].update(slots={"kind":"auto", "query":"좀비", "mode":"approach"}),
    lambda t: t["intent"].update(original_text="좀비 찾아줘"),
    lambda t: t.update(intent=None),
    lambda t: t.update(intent=[]),
))
def test_binding_rejects_translation_tampering_without_throwing(mutate):
    t = ChatClefNaturalLanguageService().translate("마을 주민 찾아줘").to_dict()
    mutate(t)
    event = SimpleNamespace(text="마을 주민 찾아줘", source="lavi_chat_ui", event_id="a"*32)
    decision = FindTranslationBindingStage(live_proof_validator=lambda *_: True).inspect(event=event, proof=object(), translation=t)
    assert decision is not None
    assert decision.reason == "find_original_request_mismatch"

@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
def test_live_proof_must_remain_valid_after_translation(source):
    t = ChatClefNaturalLanguageService().translate("주민 찾아줘").to_dict()
    event = SimpleNamespace(text="주민 찾아줘", source=source, event_id="a"*32)
    stage = FindTranslationBindingStage(live_proof_validator=lambda *_: False)
    assert stage.inspect(event=event, proof=object(), translation=t).reason == "find_live_proof_expired"


def test_logging_failure_does_not_change_binding_decision():
    t = ChatClefNaturalLanguageService().translate("주민 찾아줘").to_dict()
    def raising(_): raise RuntimeError("test logger")
    stage = FindTranslationBindingStage(live_proof_validator=lambda *_: True, log_callback=raising)
    event = SimpleNamespace(text="주민 찾아줘", source="lavi_chat_ui", event_id="a"*32)
    assert stage.inspect(event=event, proof=object(), translation=t) is None


def test_unrelated_input_cannot_be_promoted_to_find():
    t = ChatClefNaturalLanguageService().translate("주민 찾아줘").to_dict()
    stage = FindTranslationBindingStage(live_proof_validator=lambda *_: True)
    event = SimpleNamespace(text="안녕", source="lavi_chat_ui", event_id="a"*32)
    assert stage.inspect(event=event, proof=object(), translation=t).reason == "find_original_request_mismatch"
