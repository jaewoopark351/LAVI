import sounddevice as sd

from core.logger import log_print


class AudioDeviceRegistry:#20260620_kpopmodder
    def list_output_devices(self):
        result = []

        try:
            devices = sd.query_devices()

            for i, device in enumerate(devices):
                if device["max_output_channels"] > 0:
                    result.append(f"{i}: {device['name']}")

        except Exception as e:
            log_print(f"[AudioDeviceManager] output device list error: {e}")

        return result

    def list_input_devices(self):
        result = []

        try:
            devices = sd.query_devices()

            for i, device in enumerate(devices):
                if device["max_input_channels"] > 0:
                    result.append(f"{i}: {device['name']}")

        except Exception as e:
            log_print(f"[AudioDeviceManager] input device list error: {e}")

        return result

    def parse_device_id(self, label):
        try:
            if label is None:
                return None
            return int(str(label).split(":")[0])
        except Exception:
            return None

    def find_preferred_output_device(
        self,
        output_choices,
        preferred_output_keyword,
        preferred_output_id
    ):
        if preferred_output_keyword:
            for label in output_choices:
                if preferred_output_keyword in label:
                    return label

        if preferred_output_id is not None:
            for label in output_choices:
                if label.startswith(f"{preferred_output_id}:"):
                    return label

        return None
