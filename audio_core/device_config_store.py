import json
import os

from core.logger import log_print


class AudioDeviceConfigStore:#20260620_kpopmodder
    def __init__(self, config_dir, config_path):
        self.config_dir = config_dir
        self.config_path = config_path

    def save(
        self,
        output_device_label,
        input_device_label,
        tts_volume_percent=100,
    ):
        try:
            os.makedirs(self.config_dir, exist_ok=True)

            config = {
                "output_device_label": output_device_label,
                "input_device_label": input_device_label,
                "tts_volume_percent": tts_volume_percent,
            }

            with open(self.config_path, "w", encoding="utf-8") as file:
                json.dump(
                    config,
                    file,
                    ensure_ascii=False,
                    indent=4,
                )

        except Exception as e:
            log_print(f"[AudioDeviceManager] config save error: {e}")

    def load(self):
        try:
            if not os.path.exists(self.config_path):
                return None, None, 100

            with open(self.config_path, "r", encoding="utf-8") as file:
                config = json.load(file)

            output_device_label = config.get("output_device_label")
            input_device_label = config.get("input_device_label")
            tts_volume_percent = config.get("tts_volume_percent", 100)

            log_print(
                f"[AudioDeviceManager] config loaded: "
                f"output={output_device_label}, "
                f"input={input_device_label}, "
                f"tts_volume_percent={tts_volume_percent}"
            )

            return output_device_label, input_device_label, tts_volume_percent

        except Exception as e:
            log_print(f"[AudioDeviceManager] config load error: {e}")
            return None, None, 100
