import io
import os
import sys
import tempfile
import threading
import unittest
import wave
from pathlib import Path
from unittest.mock import patch


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from audio_core.device_config_store import AudioDeviceConfigStore
from audio_core.device_registry import AudioDeviceRegistry
from audio_core.playback_controller import AudioPlaybackController
from audio_device_manager import AudioDeviceManager
from tts_core.winsound_player import WinSoundAudioPlayer


class AudioDeviceManagerTests(unittest.TestCase):
    def test_manager_uses_audio_core_components(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = os.path.join(temp_dir, "audio_device_config.json")

            with (
                patch.object(AudioDeviceManager, "config_dir", temp_dir),
                patch.object(AudioDeviceManager, "config_path", config_path),
            ):
                manager = AudioDeviceManager()

        self.assertIsInstance(manager.device_registry, AudioDeviceRegistry)
        self.assertIsInstance(manager.config_store, AudioDeviceConfigStore)
        self.assertIsInstance(manager.playback_controller, AudioPlaybackController)
        self.assertIsNone(manager.output_device_id)
        self.assertIsNone(manager.input_device_id)
        self.assertEqual(100, manager.tts_volume_percent)

    def test_device_label_parsing_and_preferred_output_selection(self):
        manager = AudioDeviceManager.__new__(AudioDeviceManager)
        manager.device_registry = AudioDeviceRegistry()
        manager.preferred_output_keyword = "Speakers"
        manager.preferred_output_id = 7

        choices = [
            "3: Microsoft Sound Mapper",
            "7: USB Audio",
            "9: Speakers (Realtek)",
        ]

        self.assertEqual(manager.parse_device_id("7: USB Audio"), 7)
        self.assertIsNone(manager.parse_device_id("invalid"))
        self.assertEqual(
            manager.find_preferred_output_device(choices),
            "9: Speakers (Realtek)",
        )

        manager.preferred_output_keyword = ""
        self.assertEqual(
            manager.find_preferred_output_device(choices),
            "7: USB Audio",
        )

    def test_tts_volume_is_saved_and_clamped(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = os.path.join(temp_dir, "audio_device_config.json")

            with (
                patch.object(AudioDeviceManager, "config_dir", temp_dir),
                patch.object(AudioDeviceManager, "config_path", config_path),
            ):
                manager = AudioDeviceManager()
                manager.set_tts_volume_percent(275)

                self.assertEqual(200, manager.tts_volume_percent)
                self.assertEqual(2.0, manager.get_tts_volume_scale())

                loaded = AudioDeviceConfigStore(temp_dir, config_path).load()

        self.assertEqual((None, None, 200), loaded)

    def test_legacy_audio_config_defaults_tts_volume_to_100(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = os.path.join(temp_dir, "audio_device_config.json")
            with open(config_path, "w", encoding="utf-8") as file:
                file.write(
                    '{"output_device_label": "1: Speakers", '
                    '"input_device_label": "2: Mic"}'
                )

            loaded = AudioDeviceConfigStore(temp_dir, config_path).load()

        self.assertEqual(("1: Speakers", "2: Mic", 100), loaded)

    def test_winsound_player_scales_wav_volume_without_playing(self):
        audio_data = self._wav_bytes([1000, -1000, 30000], sample_width=2)
        player = WinSoundAudioPlayer(
            interrupt_event=threading.Event(),
            volume_scale_getter=lambda: 0.5,
        )

        scaled = player.apply_volume(audio_data)

        self.assertEqual([500, -500, 15000], self._wav_samples(scaled))

    def test_winsound_player_clips_boosted_wav_volume(self):
        audio_data = self._wav_bytes([20000, -20000], sample_width=2)
        player = WinSoundAudioPlayer(
            interrupt_event=threading.Event(),
            volume_scale_getter=lambda: 2.0,
        )

        scaled = player.apply_volume(audio_data)

        self.assertEqual([32767, -32768], self._wav_samples(scaled))

    def _wav_bytes(self, samples, sample_width):
        output = io.BytesIO()
        with wave.open(output, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(sample_width)
            wav_file.setframerate(8000)
            wav_file.writeframes(
                b"".join(
                    int(sample).to_bytes(
                        sample_width,
                        byteorder="little",
                        signed=True,
                    )
                    for sample in samples
                )
            )
        return output.getvalue()

    def _wav_samples(self, audio_data):
        with wave.open(io.BytesIO(audio_data), "rb") as wav_file:
            sample_width = wav_file.getsampwidth()
            frames = wav_file.readframes(wav_file.getnframes())
        return [
            int.from_bytes(
                frames[index:index + sample_width],
                byteorder="little",
                signed=True,
            )
            for index in range(0, len(frames), sample_width)
        ]


if __name__ == "__main__":
    unittest.main()
