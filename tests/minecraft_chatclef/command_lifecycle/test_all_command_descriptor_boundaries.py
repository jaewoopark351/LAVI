#20260915_kpopmodder: Preserve aggregated list quantities and runtime tokens through existing descriptor ownership.
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer
from tests.minecraft_chatclef.find.fixtures import descriptor
from tests.minecraft_chatclef.command_lifecycle.goto_terminal_result import test_goto_terminal_delivery as delivery


def test_duplicate_list_entries_freeze_the_actual_aggregated_target_and_count():
    result = descriptor(text="철괴 1개와 철괴 2개 구해줘")
    assert result.command == "get [iron_ingot 3]"
    assert result.target_item == "iron_ingot"
    assert result.requested_count == 3
    assert len(result.targets) == 1
    assert result.targets[0].requested_count == 3
    assert "철" in result.spoken_target_label


def test_duplicate_aggregation_never_shifts_the_following_item_label():
    result = descriptor(text="철괴 1개와 철괴 2개와 석탄 3개 구해줘")
    assert result.command == "get [iron_ingot 3, coal 3]"
    assert [(item.canonical_target, item.requested_count) for item in result.targets] == [("iron_ingot", 3), ("coal", 3)]
    assert "석탄" in result.targets[1].spoken_label
    assert result.target_item is None


def test_selected_deposit_list_is_never_described_as_the_whole_inventory():
    result = descriptor(text="철괴 1개와 석탄 3개 보관해줘")
    text = CommandLifecycleResponseRenderer().render_start(result)
    assert "전부" not in text
    assert "철" in text and "석탄" in text and "3" in text


def test_all_64_long_native_tokens_keep_feedback_without_expanding_raw_gui_limit():
    rows = [{"kind": "item", "id": "example:" + "x" * 245 + f"{n:03}",
             "translation_key": f"item.example.t{n}", "korean_name": "시험" + chr(0xAC00+n),
             "tokens": {"get": "example:" + "x" * 245 + f"{n:03}"}, "capabilities": ["get"]}
            for n in range(64)]
    snapshot = {"schema_version": 1, "minecraft_version": "1.20.1", "session_id": "s",
                "catalogue_sha256": "a" * 64, "entries": rows, "butler_user": None}
    text = "와 ".join(row["korean_name"] + " 1개" for row in rows) + " 구해줘"
    translated = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot).translate(text)
    assert translated.executable, translated.to_dict()
    assert len(translated.command) > 16384
    event = SimpleNamespace(text=text, source="lavi_chat_ui", provider_id="lavi_chat_ui",
                            event_kind="chat_submit", final=True, event_id="a" * 32)
    factory = CommandFeedbackDescriptorFactory()
    result = factory.from_trusted_translation(event=event, translation=translated.to_dict())
    assert result is not None
    assert len(result.targets) == 64
    assert result.targets[-1].spoken_label == rows[-1]["korean_name"]
    assert factory.decode_registered_command_name_only(translated.command, command_source="lavi_gui",
        event_id="a" * 32, provider_id="minecraft_gui", event_kind="raw_command_submit") is None


@pytest.mark.parametrize("command", ("auto_deposit_trust 반경 16x16", "자동보관등록 영역 16x16", "자동보관등록 반경 16x16"))
def test_native_compatibility_alias_bulk_result_reaches_existing_gui_delivery(command):
    desc = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(command,
        command_source="lavi_gui", event_id="a" * 32, provider_id="minecraft_gui", event_kind="raw_command_submit")
    assert desc is not None
    with patch.object(delivery, "descriptor", lambda _: desc):
        f = delivery._fixture("lavi_gui")
    assert f.ack.acknowledge(published=True)
    data = {"result_reason": "instant_command_observed", "result_fidelity": "command_callback_plus_native_result",
        "evidence_sequence": 1, "instant_command": {"schema_version": 1, "command": command,
            "command_name": "auto_deposit_trust", "outcome": "completed", "reason": "UPDATED",
            "values": {"registered": 2, "reenabled": 0, "already_registered": 1, "coverage_complete": True}}}
    f.graph.command_result_handler.handle(f.websocket, delivery._envelope(f, "completed", data))
    assert len(f.tts_payloads) == 1
    assert "2곳을 등록" in f.tts_payloads[0]["text"]
