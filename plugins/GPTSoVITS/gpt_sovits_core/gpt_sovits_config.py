#20260717_kpopmodder: Typed GPT-SoVITS path/config DTO kept separate from runtime startup.
import os


class GPTSoVITSConfig:
    #20260717_kpopmodder: Holds static paths/defaults so runtime classes do not recompute repository layout.
    def __init__(self, current_module_directory=None):
        self.current_module_directory = (
            current_module_directory or os.path.dirname(os.path.dirname(__file__))
        )
        self.config_dir = os.path.join(self.current_module_directory, "config")
        self.config_path = os.path.join(
            self.config_dir,
            "gpt_sovits_config.json",
        )
        self.default_config = {
            "gpt_sovits_root": "",
            "show_install_warning": True,
            "cuda_visible_devices": "1",
        }
        self.base_dir = os.path.abspath(
            os.path.join(self.current_module_directory, "..", "..")
        )
        self.gpt_sovits_ckpt_dir = os.path.join(
            self.current_module_directory,
            "gpt_sovits_ckpt_dir",
        )
        self.gpt_sovits_model_dir = os.path.join(
            self.current_module_directory,
            "gpt_sovits_model_dir",
        )
        self.default_api_url = "http://127.0.0.1:9880/tts"
        self.default_ref_audio_path = os.path.join(
            self.base_dir,
            "voices",
            "ref.wav",
        )

    def to_dict(self):
        return {
            "current_module_directory": self.current_module_directory,
            "config_dir": self.config_dir,
            "config_path": self.config_path,
            "base_dir": self.base_dir,
            "gpt_sovits_ckpt_dir": self.gpt_sovits_ckpt_dir,
            "gpt_sovits_model_dir": self.gpt_sovits_model_dir,
            "default_api_url": self.default_api_url,
            "default_ref_audio_path": self.default_ref_audio_path,
            "default_config": dict(self.default_config),
        }
