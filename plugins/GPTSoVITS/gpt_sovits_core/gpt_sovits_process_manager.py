#20260717_kpopmodder: Owns GPT-SoVITS external process startup, reuse probing, and shutdown.
import os
import time

import requests

from core.gpu_device_manager import gpu_device_manager
from core.logger import log_print
from core.process import launch_process


class GPTSoVITSProcessManager:
    #20260717_kpopmodder: Adapter boundary for process/env handling before the TTS provider calls HTTP.
    def __init__(
        self,
        config_manager,
        gpt_sovits_url,
        cuda_visible_devices="1",
    ):
        self.config_manager = config_manager
        self.gpt_sovits_url = gpt_sovits_url
        self.gpt_sovits_process = None
        self.cuda_visible_devices = str(cuda_visible_devices or "").strip()

    def probe(self):
        return {
            "server_alive": self.is_server_alive(),
            "process_running": self.is_process_running(),
            "cuda_visible_devices": self.cuda_visible_devices,
        }

    def is_process_running(self):
        process = self.gpt_sovits_process
        return bool(process is not None and process.poll() is None)

    def is_server_alive(self):
        try:
            base_url = self.gpt_sovits_url.replace("/tts", "")
            response = requests.get(
                f"{base_url}/docs",
                timeout=2
            )
            return response.status_code == 200
        except Exception:
            return False

    def start_server(self, gpt_sovits_root):
        cuda_visible_devices = str(self.cuda_visible_devices or "").strip()
        if not cuda_visible_devices:
            log_print(
                "[GPTSoVITS_TTS] ERROR: CUDA_VISIBLE_DEVICES is empty. "
                "Refusing to start/reuse GPT-SoVITS because the child "
                "process may default to GPU 0."
            )#20260627_kpopmodder: Fail closed so invalid GPU config cannot silently use cuda:0.
            self.gpt_sovits_process = None
            return

        if not self.config_manager.check_install(gpt_sovits_root):
            log_print("[GPTSoVITS_TTS] Cannot start GPT-SoVITS server.")
            return

        if self.gpt_sovits_process is not None:#20260616_kpopmodder
            return

        if self.is_server_alive():#20260616_kpopmodder
            log_print(
                "[GPTSoVITS_TTS] Existing GPT-SoVITS API server detected. Reusing."
            )
            log_print(
                "[GPTSoVITS_TTS] Existing server reused. "
                "CUDA_VISIBLE_DEVICES change requires killing old api_v2.py."
            )#20260626_kpopmodder
            self.gpt_sovits_process = None
            return

        try:
            api_script = os.path.join(gpt_sovits_root, "api_v2.py")#20260616_kpopmodder
            python_exe = os.path.join(
                gpt_sovits_root,
                "runtime",
                "python.exe"
            )

            log_print("[GPTSoVITS_TTS] Starting new GPT-SoVITS API server...")#20260616_kpopmodder
            env = os.environ.copy()#20260626_kpopmodder
            resolved_cuda_visible_devices = (
                gpu_device_manager.apply_cuda_visible_devices(
                    env,
                    "GPTSoVITS",
                    cuda_visible_devices,
                    validate=False,
                )
            )#20260717_kpopmodder
            if not resolved_cuda_visible_devices:
                log_print(
                    "[GPTSoVITS_TTS] ERROR: CUDA_VISIBLE_DEVICES is invalid. "
                    "Refusing to start GPT-SoVITS."
                )#20260717_kpopmodder
                self.gpt_sovits_process = None
                return
            log_print(
                f"[GPTSoVITS_TTS] CUDA_VISIBLE_DEVICES="
                f"{env['CUDA_VISIBLE_DEVICES']}"
            )#20260626_kpopmodder

            self.gpt_sovits_process = launch_process(
                [
                    python_exe,
                    api_script,
                    "-a",
                    "127.0.0.1",
                    "-p",
                    "9880"
                ],
                cwd=gpt_sovits_root,
                env=env,
            )

            base_url = self.gpt_sovits_url.replace("/tts", "")

            for _ in range(20):#20260616_kpopmodder
                try:
                    response = requests.get(
                        f"{base_url}/docs",
                        timeout=2
                    )

                    log_print(
                        f"[GPTSoVITS_TTS] server check: {response.status_code}"
                    )

                    if response.status_code == 200:
                        log_print("[GPTSoVITS_TTS] GPT-SoVITS API server ready.")
                        return

                except Exception as e:#20260616_kpopmodder
                    log_print(f"[GPTSoVITS_TTS] Waiting server... {e}")
                    time.sleep(1)

            log_print("[GPTSoVITS_TTS] GPT-SoVITS API server start timeout.")

        except Exception as e:#20260616_kpopmodder
            log_print(f"[GPTSoVITS_TTS] Failed to start server: {e}")

    def stop_server(self):
        try:
            if self.gpt_sovits_process:
                log_print("[GPTSoVITS_TTS] Terminating GPT-SoVITS server...")
                self.gpt_sovits_process.terminate()
                self.gpt_sovits_process = None
        except Exception as e:
            log_print(f"[GPTSoVITS_TTS] terminate failed: {e}")
