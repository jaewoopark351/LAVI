#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
import json
import os

from core.gpu_device_manager import gpu_device_manager#20260626_kpopmodder
from core.logger import log_print


class GPTSoVITSConfigManager:#20260619_kpopmodder
    def __init__(self, config_dir, config_path, default_config):
        self.config_dir = config_dir
        self.config_path = config_path
        self.default_config = default_config

    def load_root_path(self):
        env_root = str(os.getenv("GPT_SOVITS_ROOT", "") or "").strip()
        if env_root:
            log_print(
                "[GPTSoVITS_TTS] GPT-SoVITS root loaded from "
                "GPT_SOVITS_ROOT."
            )
            return env_root

        os.makedirs(self.config_dir, exist_ok=True)

        if not os.path.exists(self.config_path):
            with open(self.config_path, "w", encoding="utf-8") as file:
                json.dump(
                    self.default_config,
                    file,
                    ensure_ascii=False,
                    indent=4
                )

            log_print(
                f"[GPTSoVITS_TTS] Config created: {self.config_path}"
            )
            return self.default_config["gpt_sovits_root"]

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                config = json.load(file)

            return config.get("gpt_sovits_root", "").strip()

        except Exception as e:
            log_print(f"[GPTSoVITS_TTS] Config load failed: {e}")
            return self.default_config["gpt_sovits_root"]

    def load_cuda_visible_devices(self):#20260626_kpopmodder
        os.makedirs(self.config_dir, exist_ok=True)
        #20260626_kpopmodder: Prefer GPTSoVITS config, fallback to global GPU config.
        fallback = gpu_device_manager.get_cuda_visible_devices(
            "GPTSoVITS",
            self.default_config.get("cuda_visible_devices", "1"),
        )

        if not os.path.exists(self.config_path):
            with open(self.config_path, "w", encoding="utf-8") as file:
                json.dump(
                    self.default_config,
                    file,
                    ensure_ascii=False,
                    indent=4
                )

            log_print(
                f"[GPTSoVITS_TTS] Config created: {self.config_path}"
            )
            return fallback#20260626_kpopmodder

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                config = json.load(file)

            return gpu_device_manager.validate_cuda_visible_devices(
                config.get("cuda_visible_devices", fallback),
                "GPTSoVITS",
                default=fallback,
            )#20260626_kpopmodder

        except Exception as e:
            log_print(
                f"[GPTSoVITS_TTS] CUDA_VISIBLE_DEVICES config load failed: {e}"
            )
            return fallback#20260626_kpopmodder

    def check_install(self, gpt_sovits_root):
        if not gpt_sovits_root:
            log_print("[GPTSoVITS_TTS] GPT-SoVITS root path is empty.")
            return False

        if not os.path.exists(gpt_sovits_root):
            log_print(
                "[GPTSoVITS_TTS] GPT-SoVITS root path not found.\n"
                f"[GPTSoVITS_TTS] path: {gpt_sovits_root}\n"
                f"[GPTSoVITS_TTS] config: {self.config_path}"
            )
            return False

        api_script = os.path.join(gpt_sovits_root, "api_v2.py")
        python_exe = os.path.join(
            gpt_sovits_root,
            "runtime",
            "python.exe"
        )

        if not os.path.exists(api_script):
            log_print(f"[GPTSoVITS_TTS] api_v2.py not found: {api_script}")
            return False

        if not os.path.exists(python_exe):
            log_print(f"[GPTSoVITS_TTS] runtime python not found: {python_exe}")
            return False

        log_print(f"[GPTSoVITS_TTS] GPT-SoVITS root OK: {gpt_sovits_root}")
        return True
