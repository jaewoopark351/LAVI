import sys
import threading
import time
import unittest
import warnings
from pathlib import Path
from threading import Lock
from unittest.mock import MagicMock, patch


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


with warnings.catch_warnings():
    warnings.filterwarnings(
        "ignore",
        message="pkg_resources is deprecated as an API.*",
        category=UserWarning,
    )
    from plugins.VoiceInput.voiceInput import VoiceInput

from plugin_system.interfaces import InputPluginInterface
from core.global_state import global_state, GlobalKeys
from plugins.VoiceInput.voice_input_core.microphone_recorder import MicrophoneRecorder
from plugins.VoiceInput.voice_input_core.recorded_audio import RecordedAudio
from plugins.VoiceInput.voice_input_core.speaker_identifier import SpeakerIdentifier
from plugins.VoiceInput.voice_input_core.stt_backends import (
    TransformersWhisperBackend,
)
from plugins.VoiceInput.voice_input_core.voice_input_runtime_controller import (
    VoiceInputRuntimeController,
)
from plugins.VoiceInput.voice_input_core.voice_input_state import VoiceInputState


#20260626_kpopmodder: Cover VoiceInput audio-device and STT fallback behavior.
class DummyLiveTextbox:
    def __init__(self):
        self.messages = []

    def print(self, _message):
        self.messages.append(_message)

    def get_text(self):
        return "\n".join(str(message) for message in self.messages)


class VoiceInputImportTests(unittest.TestCase):
    def test_plugin_entry_class_imports(self):
        self.assertTrue(issubclass(VoiceInput, InputPluginInterface))

    def test_voice_input_metadata_declares_speaker_identifier_dependencies(self):
        #20260718_kpopmodder: Keep static availability metadata aligned with speaker recognition imports.
        import ast

        speaker_identifier_path = (
            PROJECT_ROOT
            / "plugins"
            / "VoiceInput"
            / "voice_input_core"
            / "speaker_identifier.py"
        )
        tree = ast.parse(
            speaker_identifier_path.read_text(encoding="utf-8"),
            filename=str(speaker_identifier_path),
        )
        imported_modules = set()
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                imported_modules.update(
                    alias.name.split(".", maxsplit=1)[0]
                    for alias in node.names
                )
            elif isinstance(node, ast.ImportFrom) and node.module:
                imported_modules.add(node.module.split(".", maxsplit=1)[0])

        required_packages = {
            str(package).lower()
            for package in VoiceInput.PLUGIN_METADATA["required_python_packages"]
        }

        self.assertIn("resemblyzer", imported_modules)
        self.assertIn("resemblyzer", required_packages)

    def test_state_defaults_are_preserved(self):
        first_state = VoiceInputState()
        second_state = VoiceInputState()

        self.assertEqual(first_state.mic_mode, "open mic")
        self.assertEqual(first_state.input_language, "korean")
        self.assertFalse(first_state.recording)
        self.assertIsNone(first_state.input_device_index)
        self.assertIsNot(first_state.mic_lock, second_state.mic_lock)

    def test_recorded_audio_returns_original_bytes(self):
        wav_bytes = b"RIFF-test-wav-data"
        audio = RecordedAudio(wav_bytes)

        self.assertIs(audio.get_wav_data(), wav_bytes)

    def test_microphone_recorder_uses_callback_device_id(self):#20260626_kpopmodder
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            input_device_index_callback=lambda: 1,
        )

        devices = [
            {"max_input_channels": 0},
            {"max_input_channels": 1},
        ]

        with patch(
            "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.query_devices",
            return_value=devices,
        ):
            self.assertEqual(recorder.get_input_device_index(), 1)

    def test_microphone_recorder_returns_none_for_missing_input_device(self):#20260626_kpopmodder
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
        )

        self.assertIsNone(recorder.get_input_device_index())

    def test_microphone_recorder_respects_callback_none_as_system_default(self):#20260626_kpopmodder
        recorder = MicrophoneRecorder(
            input_device_index=1,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            input_device_index_callback=lambda: None,
        )

        self.assertIsNone(recorder.get_input_device_index())
        self.assertIsNone(recorder.input_device_index)

    def test_microphone_recorder_falls_back_to_last_index_when_callback_fails(self):#20260626_kpopmodder
        def failing_callback():
            raise RuntimeError("audio settings unavailable")

        recorder = MicrophoneRecorder(
            input_device_index=1,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            input_device_index_callback=failing_callback,
        )

        devices = [
            {"max_input_channels": 0},
            {"max_input_channels": 1},
        ]

        with patch(
            "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.query_devices",
            return_value=devices,
        ):
            self.assertEqual(recorder.get_input_device_index(), 1)

    def test_microphone_recorder_falls_back_when_callback_device_missing(self):#20260626_kpopmodder
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            input_device_index_callback=lambda: 9,
        )

        devices = [
            {"max_input_channels": 1},
        ]

        with patch(
            "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.query_devices",
            return_value=devices,
        ):
            self.assertIsNone(recorder.get_input_device_index())

    def test_resolve_stt_device_uses_configured_device(self):#20260707_kpopmodder
        voice_input = VoiceInput()

        with patch(
            "plugins.VoiceInput.voiceInput.gpu_device_manager.validate_device",
            return_value="cuda:1",
        ) as validate_device:
            resolved_device = voice_input.resolve_stt_device("cuda:1")

        self.assertEqual(resolved_device, "cuda:1")
        validate_device.assert_called_once_with(
            "cuda:1",
            plugin_name="VoiceInput",
        )

    def test_resolve_stt_device_uses_gpu_manager_default(self):#20260707_kpopmodder
        voice_input = VoiceInput()

        with patch(
            "plugins.VoiceInput.voiceInput.gpu_device_manager.get_device",
            return_value="cuda:1",
        ) as get_device:
            resolved_device = voice_input.resolve_stt_device("")

        self.assertEqual(resolved_device, "cuda:1")
        get_device.assert_called_once_with("VoiceInput", default="cuda")

    def test_build_stt_backend_creates_transformers_backend(self):#20260707_kpopmodder
        voice_input = VoiceInput()
        settings = {
            "stt_backend": "transformers_whisper",
            "whisper_model": "openai/whisper-large-v3",
            "language": "ko",
            "torch_dtype": "float16",
        }

        backend_class = MagicMock()
        with patch.dict(
            VoiceInput.build_stt_backend.__globals__,
            {"TransformersWhisperBackend": backend_class},
        ):#20260717_kpopmodder: Patch the class globals directly because plugin loader tests may reload this module.
            backend = voice_input.build_stt_backend(
                settings=settings,
                resolved_device="cuda:1",
            )

        self.assertIs(backend, backend_class.return_value)
        backend_class.assert_called_once_with(
            model_id="openai/whisper-large-v3",
            device="cuda:1",
            torch_dtype="float16",
            language="ko",
        )

    def test_transformers_stt_backend_can_be_constructed_without_model_load(self):#20260707_kpopmodder
        with patch.object(
            TransformersWhisperBackend,
            "_load_pipeline",
            return_value=None,
        ):
            backend = TransformersWhisperBackend(
                model_id="openai/whisper-large-v3",
                device="cuda:1",
                torch_dtype="auto",
                language="ko",
            )

        self.assertEqual(backend.model_id, "openai/whisper-large-v3")
        self.assertEqual(backend.device, "cuda:1")
        self.assertEqual(backend.torch_dtype_name, "auto")
        self.assertEqual(backend.language, "ko")

    def test_transformers_stt_result_marks_confidence_unavailable(self):#20260707_kpopmodder
        with patch.object(
            TransformersWhisperBackend,
            "_load_pipeline",
            return_value=None,
        ):
            backend = TransformersWhisperBackend(
                model_id="openai/whisper-large-v3",
                device="cuda:1",
                torch_dtype="auto",
                language="ko",
            )

        result = backend._to_result(
            output={
                "text": "안녕",
                "chunks": [
                    {
                        "text": "안녕",
                        "timestamp": (0.0, 1.0),
                    }
                ],
            },
            audio_duration=1.0,
            language="ko",
        )

        self.assertEqual(result.text, "안녕")
        self.assertFalse(result.language_probability_available)
        self.assertFalse(result.avg_logprob_available)
        self.assertFalse(result.no_speech_prob_available)
        self.assertFalse(result.segments[0].avg_logprob_available)
        self.assertFalse(result.segments[0].no_speech_prob_available)

    def test_speaker_identifier_uses_configured_device(self):#20260627_kpopmodder
        with patch(
            "plugins.VoiceInput.voice_input_core.speaker_identifier.VoiceEncoder"
        ) as voice_encoder:
            with patch.object(
                SpeakerIdentifier,
                "load_reference_voices",
                return_value=None,
            ):
                SpeakerIdentifier(
                    reference_dir="unused_reference_dir",
                    threshold=0.58,
                    device="cuda:1",
                )

        voice_encoder.assert_called_once_with(device="cuda:1")

    def test_runtime_shutdown_joins_recording_thread(self):#20260628_kpopmodder
        state = VoiceInputState()
        live_textbox = DummyLiveTextbox()
        loop_entered = threading.Event()

        controller = VoiceInputRuntimeController(
            state=state,
            liveTextbox=live_textbox,
            interrupt_controller=None,
            open_mic_controller=None,
        )

        def fake_loop():
            loop_entered.set()
            while state.recording:
                time.sleep(0.01)
            controller._clear_listen_thread_if_current()

        controller.transcribe_loop = fake_loop

        with patch(
            "plugins.VoiceInput.voice_input_core.voice_input_runtime_controller.gr.Info"
        ):
            controller.start_listening()

        self.assertTrue(loop_entered.wait(timeout=1.0))
        self.assertTrue(controller._is_listen_thread_alive())

        controller.shutdown(join_timeout=1.0)

        self.assertFalse(state.recording)
        self.assertFalse(controller._is_listen_thread_alive())

    def test_runtime_start_waits_for_previous_thread_to_stop(self):#20260628_kpopmodder
        state = VoiceInputState()
        live_textbox = DummyLiveTextbox()
        controller = VoiceInputRuntimeController(
            state=state,
            liveTextbox=live_textbox,
            interrupt_controller=None,
            open_mic_controller=None,
        )
        blocker = threading.Event()
        stuck_thread = threading.Thread(target=blocker.wait)
        stuck_thread.start()
        controller._listen_thread = stuck_thread

        with patch(
            "plugins.VoiceInput.voice_input_core.voice_input_runtime_controller.gr.Info"
        ):
            controller.start_listening()

        blocker.set()
        stuck_thread.join(timeout=1.0)

        self.assertFalse(state.recording)
        self.assertIn("Listener is still stopping", live_textbox.get_text())

    def test_runtime_ignores_mic_while_song_playing(self):#20260628_kpopmodder
        class DummyInterruptController:
            def __init__(self):
                self.called = False

            def handle_ai_speaking(self):
                self.called = True

        class DummyOpenMicController:
            def __init__(self):
                self.called = False

            def handle_open_mic(self):
                self.called = True

        state = VoiceInputState()
        state.mic_mode = "open mic"
        live_textbox = DummyLiveTextbox()
        interrupt_controller = DummyInterruptController()
        open_mic_controller = DummyOpenMicController()
        controller = VoiceInputRuntimeController(
            state=state,
            liveTextbox=live_textbox,
            interrupt_controller=interrupt_controller,
            open_mic_controller=open_mic_controller,
        )

        previous_song_state = global_state.get_value(
            GlobalKeys.IS_SONG_PLAYING,
            False,
        )
        previous_ai_state = global_state.get_value(
            GlobalKeys.IS_AI_SPEAKING,
            False,
        )

        try:
            global_state.set_value(GlobalKeys.IS_SONG_PLAYING, True)
            global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)

            controller.transcribe()

            self.assertFalse(interrupt_controller.called)
            self.assertFalse(open_mic_controller.called)
        finally:
            global_state.set_value(
                GlobalKeys.IS_SONG_PLAYING,
                previous_song_state,
            )
            global_state.set_value(GlobalKeys.IS_AI_SPEAKING, previous_ai_state)

    def test_voice_input_drops_transcript_while_song_playing(self):#20260628_kpopmodder
        class DummyTranscriber:
            def transcribe(self, audio, prefix):
                return "노래 중 말"

        processed_inputs = []
        voice_input = VoiceInput()
        voice_input.transcriber = DummyTranscriber()
        voice_input.liveTextbox = DummyLiveTextbox()
        voice_input.process_input = processed_inputs.append

        previous_song_state = global_state.get_value(
            GlobalKeys.IS_SONG_PLAYING,
            False,
        )

        try:
            global_state.set_value(GlobalKeys.IS_SONG_PLAYING, True)

            voice_input.transcribe_audio_and_process(
                audio=object(),
                prefix="normal",
            )

            self.assertEqual([], processed_inputs)
            self.assertEqual("", voice_input.liveTextbox.get_text())
        finally:
            global_state.set_value(
                GlobalKeys.IS_SONG_PLAYING,
                previous_song_state,
            )


if __name__ == "__main__":
    unittest.main()
