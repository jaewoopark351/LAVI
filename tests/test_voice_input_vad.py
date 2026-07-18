#20260718_kpopmodder: Added VoiceInput VAD regression tests for Silero ONNX gating.
import hashlib
import os
import sys
import tempfile
import unittest
from pathlib import Path
from threading import Lock
from unittest.mock import patch

import numpy as np


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from plugins.VoiceInput.voice_input_core.microphone_recorder import MicrophoneRecorder
from plugins.VoiceInput.voice_input_core.stt_backends import STTResult, STTSegment
from plugins.VoiceInput.voice_input_core.vad import (
    VadModelDownloader,
    VadSettings,
    VadStateMachine,
)
from plugins.VoiceInput.voice_input_core.whisper_transcriber import WhisperTranscriber


class DummyLiveTextbox:
    def __init__(self):
        self.messages = []

    def print(self, message):
        self.messages.append(str(message))

    def get_text(self):
        return "\n".join(self.messages)


class FakeResponse:
    def __init__(self, chunks):
        self._chunks = list(chunks)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def read(self, _size):
        if not self._chunks:
            return b""
        return self._chunks.pop(0)


class FakeVadModel:
    def __init__(self, probabilities):
        self.probabilities = list(probabilities)
        self.predict_count = 0
        self.reset_count = 0

    def reset(self):
        self.reset_count += 1

    def predict(self, _frame):
        self.predict_count += 1
        if not self.probabilities:
            return 0.0
        return self.probabilities.pop(0)


class FakeInputStream:
    frames = []

    def __init__(self, *args, **kwargs):
        self.callback = kwargs["callback"]

    def __enter__(self):
        for frame in self.frames:
            self.callback(frame.reshape(-1, 1), len(frame), None, None)
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


class VoiceInputVadTests(unittest.TestCase):
    def test_settings_defaults_match_loose_silero_gate(self):
        settings = VadSettings.from_config({})

        self.assertTrue(settings.enabled)
        self.assertTrue(settings.auto_download)
        self.assertFalse(settings.legacy_fallback_enabled)
        self.assertEqual("models/vad/silero_vad_16k_op15.onnx", settings.model_path)
        self.assertEqual(16000, settings.sample_rate)
        self.assertEqual(512, settings.frame_samples)
        self.assertEqual(0.45, settings.speech_threshold)
        self.assertEqual(0.30, settings.release_threshold)
        self.assertEqual(3, settings.start_confirm_frames)
        self.assertEqual(
            "7ed98ddbad84ccac4cd0aeb3099049280713df825c610a8ed34543318f1b2c49",
            settings.model_sha256,
        )

    def test_state_machine_ignores_silence(self):
        machine = VadStateMachine(VadSettings())
        frame = np.zeros(512, dtype=np.float32)

        for _ in range(20):
            self.assertIsNone(machine.process_frame(frame, 0.0))

        self.assertIsNone(machine.flush())

    def test_state_machine_ignores_single_click(self):
        machine = VadStateMachine(VadSettings())
        frame = np.ones(512, dtype=np.float32)

        for probability in [0.9, 0.0, 0.0, 0.0]:
            self.assertIsNone(machine.process_frame(frame, probability))

        self.assertIsNone(machine.flush())

    def test_state_machine_keeps_pre_roll_for_short_speech(self):
        machine = VadStateMachine(VadSettings())
        segment = None

        probabilities = [0.5, 0.5, 0.5] + [0.1] * 24
        for index, probability in enumerate(probabilities):
            frame = np.full(512, index + 1, dtype=np.float32)
            segment = machine.process_frame(frame, probability)
            if segment is not None:
                break

        self.assertIsNotNone(segment)
        self.assertGreaterEqual(len(segment), 512 * 3)
        self.assertEqual(1.0, float(segment[0][0]))

    def test_state_machine_reset_clears_previous_speech(self):
        machine = VadStateMachine(VadSettings())
        frame = np.ones(512, dtype=np.float32)

        for _ in range(3):
            machine.process_frame(frame, 0.6)
        self.assertTrue(machine.recording)

        machine.reset()
        self.assertFalse(machine.recording)
        self.assertIsNone(machine.process_frame(frame, 0.0))

    def test_downloader_writes_model_inside_project_and_verifies_hash(self):
        payload = b"fake-onnx-model"
        expected_sha = hashlib.sha256(payload).hexdigest()
        isolation_root = PROJECT_ROOT / "test" / "test_Isolation"
        isolation_root.mkdir(parents=True, exist_ok=True)

        with tempfile.TemporaryDirectory(dir=isolation_root) as temp_dir:
            model_path = Path(temp_dir) / "models" / "vad" / "silero.onnx"
            downloader = VadModelDownloader(
                download_open_fn=lambda *args, **kwargs: FakeResponse([payload])
            )

            result = downloader.ensure_model(
                model_path=model_path,
                url="https://example.invalid/silero.onnx",
                expected_sha256=expected_sha,
                timeout_sec=1.0,
                project_root=temp_dir,
                enabled=True,
            )

            self.assertTrue(result["ok"])
            self.assertTrue(result["downloaded"])
            self.assertEqual(payload, model_path.read_bytes())

    def test_microphone_recorder_fail_closed_when_vad_unavailable(self):
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            vad_required=True,
            vad_error="missing model",
        )

        with (
            patch(
                "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.InputStream"
            ) as input_stream,
            patch("plugins.VoiceInput.voice_input_core.microphone_recorder.time.sleep"),
        ):
            audio = recorder.listen(timeout=0.01, phrase_time_limit=0.1, prefix="normal")

        self.assertIsNone(audio)
        input_stream.assert_not_called()

    def test_microphone_recorder_fail_closed_throttles_vad_unavailable_log(self):
        live_textbox = DummyLiveTextbox()
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=live_textbox,
            vad_required=True,
            vad_error="missing model",
        )

        with (
            patch("plugins.VoiceInput.voice_input_core.microphone_recorder.log_print") as log_print,
            patch("plugins.VoiceInput.voice_input_core.microphone_recorder.time.sleep"),
        ):
            recorder.listen(timeout=0.01, phrase_time_limit=0.1, prefix="normal")
            recorder.listen(timeout=0.01, phrase_time_limit=0.1, prefix="normal")

        self.assertEqual(["normal microphone VAD unavailable."], live_textbox.messages)
        self.assertEqual(1, log_print.call_count)

    def test_microphone_recorder_vad_ignores_silence(self):
        settings = VadSettings()
        FakeInputStream.frames = [np.zeros(512, dtype=np.float32) for _ in range(4)]
        vad_model = FakeVadModel([0.0, 0.0, 0.0, 0.0])
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            vad_model=vad_model,
            vad_state_machine=VadStateMachine(settings),
            vad_required=True,
        )

        with patch(
            "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.InputStream",
            FakeInputStream,
        ):
            audio = recorder.listen(timeout=0.01, phrase_time_limit=0.1, prefix="normal")

        self.assertIsNone(audio)
        self.assertGreater(vad_model.predict_count, 0)

    def test_microphone_recorder_vad_reuses_model_for_speech_segment(self):
        settings = VadSettings()
        probabilities = [0.6, 0.6, 0.6] + [0.0] * 24
        FakeInputStream.frames = [
            np.full(512, index + 1, dtype=np.float32)
            for index in range(len(probabilities))
        ]
        vad_model = FakeVadModel(probabilities)
        recorder = MicrophoneRecorder(
            input_device_index=None,
            mic_lock=Lock(),
            liveTextbox=DummyLiveTextbox(),
            vad_model=vad_model,
            vad_state_machine=VadStateMachine(settings),
            vad_required=True,
        )

        with patch(
            "plugins.VoiceInput.voice_input_core.microphone_recorder.sd.InputStream",
            FakeInputStream,
        ):
            audio = recorder.listen(timeout=0.5, phrase_time_limit=10, prefix="normal")

        self.assertIsNotNone(audio)
        self.assertEqual(1, vad_model.reset_count)
        self.assertGreater(vad_model.predict_count, 3)

    def test_whisper_transcriber_drops_repeated_numeric_hallucination(self):
        class DummyBackend:
            def transcribe(self, **_kwargs):
                text = "3, 4, " * 20
                return STTResult(
                    text=text,
                    segments=[
                        STTSegment(
                            text=text,
                            start=0.0,
                            end=3.0,
                            avg_logprob=0.0,
                            no_speech_prob=0.0,
                        )
                    ],
                    language="ko",
                    language_probability=1.0,
                )

        class DummyAudio:
            def get_wav_data(self):
                return b"RIFF-fake-wav"

        transcriber = WhisperTranscriber(
            stt_backend=DummyBackend(),
            filter_list=[],
            liveTextbox=DummyLiveTextbox(),
        )

        self.assertIsNone(transcriber.transcribe(DummyAudio(), prefix="normal"))


if __name__ == "__main__":
    unittest.main()
