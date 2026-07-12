import json
import os

from plugin_system.interfaces import InputPluginInterface
from ui_core.live_textbox import LiveTextbox
from audio_device_manager import audio_device_manager#20260625_kpopmodder: Follow saved Audio Settings input device.
from core.config_manager import config_manager
from core.gpu_device_manager import gpu_device_manager#20260626_kpopmodder
from core.global_state import global_state, GlobalKeys#20260628_kpopmodder
from core.logger import log_print#20260626_kpopmodder

#20260620_kpopmodder: Import grouped VoiceInput helpers from voice_input_core.
from .voice_input_core.languages import LANGUAGES
from .voice_input_core.speaker_identifier import SpeakerIdentifier
from .voice_input_core.whisper_transcriber import WhisperTranscriber
from .voice_input_core.speaker_service import SpeakerService
from .voice_input_core.microphone_recorder import MicrophoneRecorder
from .voice_input_core.interrupt_controller import InterruptController
from .voice_input_core.open_mic_controller import OpenMicController
from .voice_input_core.stt_backends import TransformersWhisperBackend

from .voice_input_core.voice_input_state import VoiceInputState
from .voice_input_core.voice_input_ui_controller import VoiceInputUiController
from .voice_input_core.voice_input_runtime_controller import VoiceInputRuntimeController
from .voice_input_core.voice_input_hotkey_controller import VoiceInputHotkeyController


class VoiceInput(InputPluginInterface):#20260618_kpopmodder
    current_module_directory = os.path.dirname(__file__)

    key_to_bind = "ctrl+shift+alt+q"#20260618_kpopmodder

    def load_stt_json_settings(self):#20260707_kpopmodder
        config_path = os.path.abspath(
            os.path.join(
                self.current_module_directory,
                "..",
                "..",
                "config",
                "voice_input_config.json",
            )
        )

        if not os.path.exists(config_path):
            return {}

        try:
            with open(config_path, "r", encoding="utf-8") as file:
                loaded_config = json.load(file)

            if not isinstance(loaded_config, dict):
                raise ValueError("top-level JSON value must be an object")

            section = loaded_config.get("VoiceInput", loaded_config)

            if not isinstance(section, dict):
                raise ValueError("VoiceInput section must be an object")

            return {
                str(key).lower(): value
                for key, value in section.items()
            }

        except Exception as e:
            log_print(
                "[VoiceInput] voice_input_config.json load failed. "
                f"using defaults/config.ini values. error={e}"
            )
            return {}

    def load_stt_settings(self):#20260707_kpopmodder: VoiceInput STT settings can switch backend/model without code edits.
        config = self.load_stt_json_settings()
        config.update(config_manager.load_section("VoiceInput"))

        return {
            "stt_backend": (
                config.get("stt_backend")
                or "transformers_whisper"
            ),
            "whisper_model": (
                config.get("whisper_model")
                or "openai/whisper-large-v3-turbo"
            ),
            "language": self.normalize_stt_language(
                config.get("language") or "ko"
            ),
            "torch_dtype": config.get("torch_dtype") or "auto",
            "device": config.get("device") or "",
        }

    def normalize_stt_language(self, language):#20260707_kpopmodder
        text = str(language or "").strip()

        if not text:
            return "ko"

        lowered = text.lower()
        if lowered in ("auto", "none", "null"):
            return "auto"

        if lowered in LANGUAGES:
            return lowered

        for code, name in LANGUAGES.items():
            if lowered == str(name).lower():
                return code

        return lowered

    def stt_language_to_ui_value(self, language):#20260707_kpopmodder
        normalized = self.normalize_stt_language(language)

        if normalized == "auto":
            return "auto"

        return LANGUAGES.get(normalized, normalized)

    def resolve_transcribe_language(self):#20260707_kpopmodder
        return self.normalize_stt_language(self.state.input_language)

    def resolve_stt_device(self, configured_device=""):#20260626_kpopmodder
        if configured_device:
            return gpu_device_manager.validate_device(
                configured_device,
                plugin_name="VoiceInput",
            )

        resolved_device = gpu_device_manager.get_device(
            "VoiceInput",
            default="cuda",
        )
        return resolved_device

    def build_stt_backend(
        self,
        settings,
        resolved_device,
    ):#20260707_kpopmodder
        backend_name = str(settings.get("stt_backend") or "").strip().lower()

        if backend_name != "transformers_whisper":
            raise ValueError(
                "Unsupported VoiceInput stt_backend: "
                f"{backend_name!r}. Use 'transformers_whisper'."
            )

        return TransformersWhisperBackend(
            model_id=settings.get("whisper_model"),
            device=resolved_device,
            torch_dtype=settings.get("torch_dtype"),
            language=settings.get("language"),
        )

    def init(self):
        self.liveTextbox = LiveTextbox()
        self.state = VoiceInputState()
        self.stt_settings = self.load_stt_settings()
        self.state.input_language = self.stt_language_to_ui_value(
            self.stt_settings.get("language")
        )#20260707_kpopmodder
        self.sync_input_device_from_audio_settings()#20260625_kpopmodder

        resolved_device = self.resolve_stt_device(
            self.stt_settings.get("device")
        )#20260626_kpopmodder
        self.liveTextbox.print(f"[VoiceInput] resolved device={resolved_device}")
        log_print(f"[VoiceInput] resolved device={resolved_device}")#20260626_kpopmodder

        self.stt_backend = self.build_stt_backend(
            settings=self.stt_settings,
            resolved_device=resolved_device,
        )

        reference_dir = os.path.join(
            self.current_module_directory,
            "reference_voices"
        )

        self.speaker_identifier = SpeakerIdentifier(
            reference_dir=reference_dir,
            threshold=0.58,
            device=resolved_device#20260627_kpopmodder: Keep speaker recognition on the same GPU as VoiceInput.
        )

        self.liveTextbox.print("Transformers Whisper backend loaded")
        log_print(
            "[VoiceInput] STT backend loaded: "
            f"backend={self.stt_settings.get('stt_backend')}, "
            f"model={self.stt_settings.get('whisper_model')}, "
            f"device={resolved_device}, "
            f"torch_dtype={self.stt_settings.get('torch_dtype')}"
        )#20260707_kpopmodder

        self.whisper_filter_list = [
            "you",
            "thank you.",
            "thanks for watching.",
            "Thank you for watching.",
            "감사합니다",
            "감사합니다.",
            "시청해 주셔서 감사합니다.",
            "시청해주셔서 감사합니다.",
            "1.5%",
            "I'm going to put it in the fridge.",
            "I",
            ".",
            "okay.",
            "bye.",
            "so,"
        ]

        self.recorder = MicrophoneRecorder(
            input_device_index=self.state.input_device_index,
            mic_lock=self.state.mic_lock,
            liveTextbox=self.liveTextbox,
            input_device_index_callback=self.get_configured_input_device_index#20260625_kpopmodder
        )

        self.transcriber = WhisperTranscriber(
            stt_backend=self.stt_backend,
            filter_list=self.whisper_filter_list,
            liveTextbox=self.liveTextbox,
            language_callback=self.resolve_transcribe_language
        )

        self.speaker_service = SpeakerService(
            speaker_identifier=self.speaker_identifier
        )

        self.interrupt_controller = InterruptController(
            recorder=self.recorder,
            speaker_service=self.speaker_service,
            transcribe_audio_callback=self.transcribe_audio_and_process,
            liveTextbox=self.liveTextbox,
            get_recording_callback=lambda: self.state.recording,
            get_last_interrupt_check_time_callback=lambda: self.state.last_interrupt_check_time,
            set_last_interrupt_check_time_callback=self.set_last_interrupt_check_time,
            get_last_interrupt_time_callback=lambda: self.state.last_interrupt_time,
            set_last_interrupt_time_callback=self.set_last_interrupt_time
        )

        self.open_mic_controller = OpenMicController(
            recorder=self.recorder,
            speaker_service=self.speaker_service,
            transcribe_audio_callback=self.transcribe_audio_and_process,
            liveTextbox=self.liveTextbox
        )

        self.runtime_controller = VoiceInputRuntimeController(
            state=self.state,
            liveTextbox=self.liveTextbox,
            interrupt_controller=self.interrupt_controller,
            open_mic_controller=self.open_mic_controller
        )

        self.ui_controller = VoiceInputUiController(
            liveTextbox=self.liveTextbox,
            get_input_language_callback=lambda: self.state.input_language,
            start_listening_callback=self.start_listening,
            stop_listening_callback=self.stop_listening,
            on_language_change_callback=self.on_language_change
        )

        self.hotkey_controller = VoiceInputHotkeyController(
            key_to_bind=self.key_to_bind,
            liveTextbox=self.liveTextbox
        )
        self.hotkey_controller.register()

    def get_configured_input_device_index(self):#20260625_kpopmodder
        input_device_id = audio_device_manager.get_input_device_id()

        if input_device_id is not None:
            self.state.input_device_index = input_device_id
            return input_device_id

        self.state.input_device_index = None
        return None

    def sync_input_device_from_audio_settings(self):#20260625_kpopmodder
        try:
            input_device_id = audio_device_manager.get_input_device_id()
        except Exception as e:
            self.liveTextbox.print(
                f"[VoiceInput] Audio Settings input device read failed: {e}"
            )
            return

        if input_device_id is None:
            self.liveTextbox.print(
                "[VoiceInput] Input device fallback: system default"
            )
            self.state.input_device_index = None
            return

        self.state.input_device_index = input_device_id
        self.liveTextbox.print(
            f"[VoiceInput] Input device from Audio Settings: {input_device_id}"
        )

    def create_ui(self):
        self.ui_controller.create_ui()

    def on_language_change(self, choice):
        self.state.input_language = choice
        self.liveTextbox.print(f"changed language to {choice}")
        return self.liveTextbox.get_text()

    def start_listening(self):
        return self.runtime_controller.start_listening()

    def stop_listening(self):
        return self.runtime_controller.stop_listening()

    def transcribe_audio_and_process(self, audio, prefix="interrupt"):
        transcribed_text = self.transcriber.transcribe(
            audio=audio,
            prefix=prefix
        )

        if transcribed_text is None:
            return

        if global_state.get_value(GlobalKeys.IS_SONG_PLAYING, False):
            log_print(
                f"[VoiceInput] song playing state blocked {prefix} transcript."
            )#20260628_kpopmodder
            return

        self.liveTextbox.print(f"{prefix} transcribed output: {transcribed_text}")
        self.process_input(transcribed_text)

    def set_last_interrupt_check_time(self, value):
        self.state.last_interrupt_check_time = value

    def set_last_interrupt_time(self, value):
        self.state.last_interrupt_time = value

    def shutdown(self):#20260623_kpopmodder
        try:
            runtime_controller = getattr(self, "runtime_controller", None)
            if runtime_controller is not None:
                runtime_controller.shutdown()#20260628_kpopmodder: Join the recording thread after asking the mic loop to stop.
            else:
                self.state.recording = False
                self.state.ambience_adjusted = False
            self.liveTextbox.print("[VoiceInput] Shutdown: stopped listening.")
        except Exception:
            pass

        hotkey_controller = getattr(self, "hotkey_controller", None)
        if hotkey_controller is not None:
            hotkey_controller.shutdown()
