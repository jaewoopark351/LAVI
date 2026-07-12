#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
import os

import requests

from core.logger import log_print


class GPTSoVITSModelManager:#20260619_kpopmodder
    def __init__(self, gpt_sovits_ckpt_dir, gpt_sovits_model_dir, gpt_sovits_url):
        self.gpt_sovits_ckpt_dir = gpt_sovits_ckpt_dir
        self.gpt_sovits_model_dir = gpt_sovits_model_dir
        self.gpt_sovits_url = gpt_sovits_url

        self.gpt_weight_path = ""
        self.sovits_weight_path = ""

        self.gpt_ckpt_names = []
        self.sovits_model_names = []

    def ensure_directories(self):
        os.makedirs(self.gpt_sovits_ckpt_dir, exist_ok=True)
        os.makedirs(self.gpt_sovits_model_dir, exist_ok=True)

    def update_model_list(self):
        self.gpt_ckpt_names = []
        self.sovits_model_names = []

        if os.path.exists(self.gpt_sovits_ckpt_dir):
            for name in os.listdir(self.gpt_sovits_ckpt_dir):
                if name.endswith(".ckpt"):
                    self.gpt_ckpt_names.append(name)

        if os.path.exists(self.gpt_sovits_model_dir):
            for name in os.listdir(self.gpt_sovits_model_dir):
                if name.endswith(".pth"):
                    self.sovits_model_names.append(name)

        self.gpt_ckpt_names.sort()
        self.sovits_model_names.sort()

        if self.gpt_ckpt_names:
            self.gpt_weight_path = os.path.join(
                self.gpt_sovits_ckpt_dir,
                self.gpt_ckpt_names[0]
            )

        if self.sovits_model_names:
            self.sovits_weight_path = os.path.join(
                self.gpt_sovits_model_dir,
                self.sovits_model_names[0]
            )

    def load_weights(self):
        try:
            base_url = self.gpt_sovits_url.replace("/tts", "")

            if self.gpt_weight_path and os.path.exists(self.gpt_weight_path):
                response = requests.get(
                    f"{base_url}/set_gpt_weights",
                    params={"weights_path": self.gpt_weight_path},
                    timeout=60
                )

                log_print(
                    f"[GPTSoVITS_TTS] GPT weight load: "
                    f"{response.status_code} {self.gpt_weight_path}"
                )
            else:
                log_print(
                    f"[GPTSoVITS_TTS] GPT weight not found: {self.gpt_weight_path}"
                )

            if self.sovits_weight_path and os.path.exists(self.sovits_weight_path):
                response = requests.get(
                    f"{base_url}/set_sovits_weights",
                    params={"weights_path": self.sovits_weight_path},
                    timeout=60
                )

                log_print(
                    f"[GPTSoVITS_TTS] SoVITS weight load: "
                    f"{response.status_code} {self.sovits_weight_path}"
                )
            else:
                log_print(
                    f"[GPTSoVITS_TTS] SoVITS weight not found: "
                    f"{self.sovits_weight_path}"
                )

        except Exception as e:
            log_print(f"[GPTSoVITS_TTS] Weight load failed: {e}")

    def set_gpt_weight_by_name(self, name):
        if not name:
            return

        self.gpt_weight_path = os.path.join(
            self.gpt_sovits_ckpt_dir,
            name
        )
        self.load_weights()

    def set_sovits_weight_by_name(self, name):
        if not name:
            return

        self.sovits_weight_path = os.path.join(
            self.gpt_sovits_model_dir,
            name
        )
        self.load_weights()
