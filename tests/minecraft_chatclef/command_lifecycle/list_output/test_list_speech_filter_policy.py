#20260915_kpopmodder: Keep normal TTS filtering and distinguish manual synthesis from lifecycle queueing.
from types import SimpleNamespace

import pytest

import safety_filter
from llm_core.routed_response.presentation.routed_response_text import RoutedResponseText
from tests.minecraft_chatclef.command_lifecycle.list_output.test_trusted_destination_list_delivery import (
    IDENTIFIER, PHRASE, drain_speech, setup,
)
from tts_core import TTS


@pytest.mark.parametrize("source", ("external", "minecraft_chatclef"))
def test_generic_dual_text_or_source_labels_never_exempt_speech_from_filtering(source):
    f = setup()
    f.publisher.emit_external_response(RoutedResponseText(IDENTIFIER, IDENTIFIER), source=source)
    queued = drain_speech(f)
    assert queued
    assert IDENTIFIER not in " ".join(item["text"] for item in queued)
    assert "검열됨" in " ".join(item["text"] for item in queued)


def test_user_mapping_does_not_become_a_structured_speech_contract():
    f = setup()
    f.publisher.emit_external_response({"display_text": IDENTIFIER, "speech_text": "forged"})
    assert "speech_text" in f.outputs[-1]["text"]
    assert IDENTIFIER not in " ".join(item["text"] for item in drain_speech(f))


def test_lower_user_filter_length_remains_authoritative(monkeypatch):
    f = setup()
    config = dict(safety_filter._safety_filter.config, max_text_length=32)
    monkeypatch.setattr(safety_filter._safety_filter, "config", config)
    f.publisher.emit_external_response(RoutedResponseText("화면 전체 목록", "가" * 500))
    assert sum(len(item["text"]) for item in drain_speech(f)) <= 32


def test_manual_wrapper_filters_separately_and_lifecycle_queues_direct_plugin_callback():
    f = setup()
    synthesis, subtitles, audio = [], [], []
    f.tts.current_plugin = SimpleNamespace(synthesize=lambda text: synthesis.append(text) or b"fixture")
    f.tts.update_subtitle_file = subtitles.append
    f.tts.play_sound_from_bytes = audio.append
    assert TTS.wrapper_synthesize(f.tts, IDENTIFIER) == b"fixture"
    assert IDENTIFIER not in synthesis[-1] and "검열됨" in synthesis[-1]
    assert subtitles == synthesis and audio == [b"fixture"]
    f.route.dispatch(PHRASE)
    assert f.tts.process_calls[-1] is f.tts.current_plugin.synthesize
    assert len(synthesis) == 1
