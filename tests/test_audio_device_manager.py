import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from audio_core.device_config_store import AudioDeviceConfigStore
from audio_core.device_registry import AudioDeviceRegistry
from audio_core.playback_controller import AudioPlaybackController
from audio_device_manager import AudioDeviceManager


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


if __name__ == "__main__":
    unittest.main()
