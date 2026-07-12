#20260620_kpopmodder: Config, playback, and device imports moved to audio_core helpers.
# import json
import os
# import threading
# import time

import gradio as gr
# import sounddevice as sd

from core.logger import log_print
from audio_core.device_config_store import AudioDeviceConfigStore
from audio_core.device_registry import AudioDeviceRegistry
from audio_core.playback_controller import AudioPlaybackController


class AudioDeviceManager:  # 20260614_kpopmodder
    current_module_directory = os.path.dirname(__file__)

    preferred_output_keyword = ""#20260614_kpopmodder
    preferred_output_id = None

    config_dir = os.path.join(
        current_module_directory,
        "config"
    )

    config_path = os.path.join(
        config_dir,
        "audio_device_config.json"
    )

    def __init__(self):
        self.output_device_id = None
        self.input_device_id = None

        self.output_device_label = None
        self.input_device_label = None

        #20260620_kpopmodder: Playback state moved to AudioPlaybackController.
        # self.play_lock = threading.RLock()
        # self.is_playing = False

        self.device_registry = AudioDeviceRegistry()#20260620_kpopmodder
        self.config_store = AudioDeviceConfigStore(
            config_dir=self.config_dir,
            config_path=self.config_path
        )#20260620_kpopmodder
        self.playback_controller = AudioPlaybackController()#20260620_kpopmodder

        self.load_config()

    @property
    def play_lock(self):
        return self.playback_controller.play_lock

    @play_lock.setter
    def play_lock(self, value):
        self.playback_controller.play_lock = value

    @property
    def is_playing(self):
        return self.playback_controller.is_playing

    @is_playing.setter
    def is_playing(self, value):
        self.playback_controller.is_playing = bool(value)

    def list_output_devices(self):
        return self.device_registry.list_output_devices()

        #20260620_kpopmodder: Output device discovery moved to AudioDeviceRegistry.
        # result = []

        # try:
        #     devices = sd.query_devices()

        #     for i, device in enumerate(devices):
        #         if device["max_output_channels"] > 0:
        #             result.append(f"{i}: {device['name']}")

        # except Exception as e:
        #     log_print(f"[AudioDeviceManager] output device list error: {e}")

        # return result

    def list_input_devices(self):
        return self.device_registry.list_input_devices()

        #20260620_kpopmodder: Input device discovery moved to AudioDeviceRegistry.
        # result = []

        # try:
        #     devices = sd.query_devices()

        #     for i, device in enumerate(devices):
        #         if device["max_input_channels"] > 0:
        #             result.append(f"{i}: {device['name']}")

        # except Exception as e:
        #     log_print(f"[AudioDeviceManager] input device list error: {e}")

        # return result

    def parse_device_id(self, label):
        return self.device_registry.parse_device_id(label)

        #20260620_kpopmodder: Device label parsing moved to AudioDeviceRegistry.
        # try:
        #     if label is None:
        #         return None
        #     return int(str(label).split(":")[0])
        # except Exception:
        #     return None

    def find_preferred_output_device(self, output_choices):#20260614_kpopmodder
        return self.device_registry.find_preferred_output_device(
            output_choices=output_choices,
            preferred_output_keyword=self.preferred_output_keyword,
            preferred_output_id=self.preferred_output_id
        )

        #20260620_kpopmodder: Preferred-device matching moved to AudioDeviceRegistry.
        # if self.preferred_output_keyword:
        #     for label in output_choices:
        #         if self.preferred_output_keyword in label:
        #             return label

        # if self.preferred_output_id is not None:
        #     for label in output_choices:
        #         if label.startswith(f"{self.preferred_output_id}:"):
        #             return label

        # return None

    # def find_preferred_output_device(self, output_choices):#20260614_kpopmodder
    #     for label in output_choices:
    #         if self.preferred_output_keyword in label:
    #             return label

    #     for label in output_choices:
    #         if label.startswith(f"{self.preferred_output_id}:"):
    #             return label

    #     return None

    def set_output_device(self, label):
        if not label:
            return

        with self.play_lock:
            self.playback_controller.stop_before_device_change()

            self.output_device_label = label
            self.output_device_id = self.parse_device_id(label)

            log_print(f"[AudioDeviceManager] output device set: {label}")
            self.save_config()

        #20260620_kpopmodder: Playback stop handling moved to AudioPlaybackController.
        # with self.play_lock:
        #     try:
        #         if self.is_playing:
        #             log_print("[AudioDeviceManager] changing output during playback. stopping first.")
        #             sd.stop()
        #             time.sleep(0.2)
        #             self.is_playing = False
        #     except Exception as e:
        #         log_print(f"[AudioDeviceManager] stop before device change error: {e}")

        #     self.output_device_label = label
        #     self.output_device_id = self.parse_device_id(label)

        #     log_print(f"[AudioDeviceManager] output device set: {label}")
        #     self.save_config()

    def set_input_device(self, label):
        if not label:
            return

        self.input_device_label = label
        self.input_device_id = self.parse_device_id(label)

        log_print(f"[AudioDeviceManager] input device set: {label}")
        self.save_config()

    def get_default_output_value(self):
        output_choices = self.list_output_devices()

        preferred_label = self.find_preferred_output_device(output_choices)

        if preferred_label:
            self.set_output_device(preferred_label)
            return preferred_label

        if self.output_device_label in output_choices:
            self.output_device_id = self.parse_device_id(self.output_device_label)
            return self.output_device_label

        if output_choices:
            self.set_output_device(output_choices[0])
            return output_choices[0]

        return None

    def get_default_input_value(self):
        input_choices = self.list_input_devices()

        if self.input_device_label in input_choices:
            self.input_device_id = self.parse_device_id(self.input_device_label)
            return self.input_device_label

        self.input_device_id = None#20260625_kpopmodder: Missing saved input label means use system default input.

        return None

    def create_ui(self):
        with gr.Tab("Audio Settings"):#20260615_kpopmodder
            with gr.Accordion("Audio Devices", open=True):
                output_choices = self.list_output_devices()
                input_choices = self.list_input_devices()

                self.output_device_dropdown = gr.Dropdown(
                    label="Output Device",
                    choices=output_choices,
                    value=self.get_default_output_value(),
                    interactive=True,
                )

                self.input_device_dropdown = gr.Dropdown(
                    label="Input Device",
                    choices=input_choices,
                    value=self.get_default_input_value(),
                    interactive=True,
                )

                self.refresh_audio_device_button = gr.Button(
                    "Refresh Audio Devices"
                )

            self.output_device_dropdown.change(
                self.set_output_device,
                inputs=self.output_device_dropdown,
                outputs=[],
            )

            self.input_device_dropdown.change(
                self.set_input_device,
                inputs=self.input_device_dropdown,
                outputs=[],
            )

            self.refresh_audio_device_button.click(
                self.refresh_ui,
                outputs=[
                    self.output_device_dropdown,
                    self.input_device_dropdown,
                ],
            )

    def refresh_ui(self):
        output_choices = self.list_output_devices()
        input_choices = self.list_input_devices()

        output_value = None
        input_value = None

        preferred_label = self.find_preferred_output_device(output_choices)

        if preferred_label:
            output_value = preferred_label
            self.set_output_device(output_value)
        elif self.output_device_label in output_choices:
            output_value = self.output_device_label
            self.output_device_id = self.parse_device_id(output_value)
        elif output_choices:
            output_value = output_choices[0]
            self.set_output_device(output_value)

        if self.input_device_label in input_choices:
            input_value = self.input_device_label
            self.input_device_id = self.parse_device_id(input_value)
        else:
            self.input_device_id = None#20260625_kpopmodder: Do not auto-save a PC-specific input device.

        return (
            gr.update(
                choices=output_choices,
                value=output_value,
            ),
            gr.update(
                choices=input_choices,
                value=input_value,
            ),
        )

    def play(self, audio_data, sample_rate):#20260614_kpopmodder
        self.playback_controller.play(audio_data, sample_rate)

        #20260620_kpopmodder: Default-device playback moved to AudioPlaybackController.
        # with self.play_lock:
        #     try:
        #         self.is_playing = True

        #         # 안정화 테스트:
        #         # 특정 output_device_id를 직접 지정하지 않고
        #         # Windows 기본 출력 장치로 재생한다.
        #         sd.play(
        #             audio_data,
        #             sample_rate,
        #         )

        #         sd.wait()

        #     except Exception as e:
        #         log_print(f"[AudioDeviceManager] playback error: {e}")

        #     finally:
        #         self.is_playing = False

    # def play(self, audio_data, sample_rate):#20260614_kpopmodder
    #     with self.play_lock:
    #         try:
    #             self.is_playing = True

    #             output_choices = self.list_output_devices()
    #             preferred_label = self.find_preferred_output_device(output_choices)

    #             if preferred_label:
    #                 self.output_device_label = preferred_label
    #                 self.output_device_id = self.parse_device_id(preferred_label)

    #             if self.output_device_id is not None:
    #                 sd.play(
    #                     audio_data,
    #                     sample_rate,
    #                     device=self.output_device_id,
    #                 )
    #             else:
    #                 sd.play(audio_data, sample_rate)

    #             sd.wait()

    #         except Exception as e:
    #             log_print(f"[AudioDeviceManager] playback error: {e}")

    #         finally:
    #             self.is_playing = False

    def stop(self):
        self.playback_controller.stop()

        #20260620_kpopmodder: Playback stop handling moved to AudioPlaybackController.
        # with self.play_lock:
        #     try:
        #         sd.stop()
        #         self.is_playing = False
        #     except Exception as e:
        #         log_print(f"[AudioDeviceManager] stop error: {e}")

    def save_config(self):
        self.config_store.save(
            output_device_label=self.output_device_label,
            input_device_label=self.input_device_label
        )

        #20260620_kpopmodder: Audio device config persistence moved to AudioDeviceConfigStore.
        # try:
        #     os.makedirs(self.config_dir, exist_ok=True)

        #     config = {
        #         "output_device_label": self.output_device_label,
        #         "input_device_label": self.input_device_label,
        #     }

        #     with open(self.config_path, "w", encoding="utf-8") as f:
        #         json.dump(
        #             config,
        #             f,
        #             ensure_ascii=False,
        #             indent=4,
        #         )

        # except Exception as e:
        #     log_print(f"[AudioDeviceManager] config save error: {e}")

    def load_config(self):
        (
            self.output_device_label,
            self.input_device_label
        ) = self.config_store.load()

        self.output_device_id = self.parse_device_id(self.output_device_label)
        self.input_device_id = None#20260625_kpopmodder: Validate saved input label against current devices before use.

        #20260620_kpopmodder: Audio device config loading moved to AudioDeviceConfigStore.
        # try:
        #     if not os.path.exists(self.config_path):
        #         return

        #     with open(self.config_path, "r", encoding="utf-8") as f:
        #         config = json.load(f)

        #     self.output_device_label = config.get("output_device_label")
        #     self.input_device_label = config.get("input_device_label")

        #     self.output_device_id = self.parse_device_id(self.output_device_label)
        #     self.input_device_id = self.parse_device_id(self.input_device_label)

        #     log_print(
        #         f"[AudioDeviceManager] config loaded: "
        #         f"output={self.output_device_label}, "
        #         f"input={self.input_device_label}"
        #     )

        # except Exception as e:
        #     log_print(f"[AudioDeviceManager] config load error: {e}")

    def get_output_device_id(self):  # 20260614_kpopmodder
        return self.output_device_id

    def get_input_device_id(self):  # 20260614_kpopmodder
        input_choices = self.list_input_devices()#20260625_kpopmodder

        if self.input_device_label in input_choices:
            self.input_device_id = self.parse_device_id(self.input_device_label)
            return self.input_device_id

        if self.input_device_label:
            log_print(
                "[AudioDeviceManager] saved input device unavailable. "
                "VoiceInput will use system default input."
            )#20260625_kpopmodder

        self.input_device_id = None
        return None


audio_device_manager = AudioDeviceManager()
