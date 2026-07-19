import os

#20260620_kpopmodder: Import grouped GPTSoVITS helpers from gpt_sovits_core.
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_client import GPTSoVITSClient
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config import GPTSoVITSConfig
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config_manager import GPTSoVITSConfigManager
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_model_manager import GPTSoVITSModelManager
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager import GPTSoVITSProcessManager


class GPTSoVITSTTS:#20260615_kpopmodder
    current_module_directory = os.path.dirname(__file__)

    config_dir = os.path.join(current_module_directory, "config")
    config_path = os.path.join(config_dir, "gpt_sovits_config.json")

    default_config = {
        "gpt_sovits_root": "",
        "show_install_warning": True,
        "cuda_visible_devices": "1"#20260626_kpopmodder
    }

    base_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "..")
    )

    gpt_sovits_ckpt_dir = os.path.join(
        current_module_directory,
        "gpt_sovits_ckpt_dir"
    )

    gpt_sovits_model_dir = os.path.join(
        current_module_directory,
        "gpt_sovits_model_dir"
    )

    def __init__(self):
        #20260717_kpopmodder: Keep static path/default decisions in GPTSoVITSConfig.
        self.config = GPTSoVITSConfig(self.current_module_directory)
        self._gpt_sovits_url = self.config.default_api_url
        self.gpt_sovits_root = ""
        #20260626_kpopmodder: Pass GPU pinning to the GPT-SoVITS child process only.
        self.cuda_visible_devices = str(
            self.config.default_config.get("cuda_visible_devices", "1")
        ).strip()#20260626_kpopmodder

        self.text_language = "ko"
        self.prompt_language = "ko"
        #20260720_kpopmodder: Expose GPT-SoVITS API inference controls without changing server defaults.
        self.speed_factor = 1.0
        self.top_k = 15
        self.top_p = 1.0
        self.temperature = 1.0
        self.repetition_penalty = 1.35

        self.prompt_text = (
            "진짜 이때만큼 흥분했던 기억이 없는 것 같아요.\n"
            "이 방송 이후로 1년이 지났지만 여전히 최고의 기억입니다."
        )

        self.ref_audio_path = self.config.default_ref_audio_path

        self.config_manager = GPTSoVITSConfigManager(
            config_dir=self.config.config_dir,
            config_path=self.config.config_path,
            default_config=self.config.default_config
        )
        self.process_manager = GPTSoVITSProcessManager(
            config_manager=self.config_manager,
            gpt_sovits_url=self._gpt_sovits_url,
            cuda_visible_devices=self.cuda_visible_devices#20260626_kpopmodder
        )
        self.server_manager = self.process_manager
        self.model_manager = GPTSoVITSModelManager(
            gpt_sovits_ckpt_dir=self.config.gpt_sovits_ckpt_dir,
            gpt_sovits_model_dir=self.config.gpt_sovits_model_dir,
            gpt_sovits_url=self._gpt_sovits_url
        )
        self.api_client = GPTSoVITSClient(
            current_module_directory=self.current_module_directory,
            gpt_sovits_url=self._gpt_sovits_url
        )

    @property
    def gpt_sovits_url(self):
        return self._gpt_sovits_url

    @gpt_sovits_url.setter
    def gpt_sovits_url(self, value):
        self._gpt_sovits_url = value

        if hasattr(self, "server_manager"):
            self.server_manager.gpt_sovits_url = value
        if hasattr(self, "model_manager"):
            self.model_manager.gpt_sovits_url = value
        if hasattr(self, "api_client"):
            self.api_client.gpt_sovits_url = value

    @property
    def gpt_sovits_process(self):
        return self.server_manager.gpt_sovits_process

    @gpt_sovits_process.setter
    def gpt_sovits_process(self, value):
        self.server_manager.gpt_sovits_process = value

    @property
    def gpt_weight_path(self):
        return self.model_manager.gpt_weight_path

    @gpt_weight_path.setter
    def gpt_weight_path(self, value):
        self.model_manager.gpt_weight_path = value

    @property
    def sovits_weight_path(self):
        return self.model_manager.sovits_weight_path

    @sovits_weight_path.setter
    def sovits_weight_path(self, value):
        self.model_manager.sovits_weight_path = value

    @property
    def gpt_ckpt_names(self):
        return self.model_manager.gpt_ckpt_names

    @property
    def sovits_model_names(self):
        return self.model_manager.sovits_model_names

    def init(self):
        self.model_manager.ensure_directories()

        self.load_config()
        self.update_model_list()
        self.start_server()
        self.load_weights()

    def load_config(self):
        self.gpt_sovits_root = self.config_manager.load_root_path()
        self.cuda_visible_devices = (
            self.config_manager.load_cuda_visible_devices()
        )#20260626_kpopmodder
        self.server_manager.cuda_visible_devices = self.cuda_visible_devices#20260626_kpopmodder

    def check_install(self):
        return self.config_manager.check_install(self.gpt_sovits_root)

    def is_gpt_sovits_server_alive(self):#20260616_kpopmodder
        return self.server_manager.is_server_alive()

    # def start_server(self):
    #     if not self.check_install():
    #         log_print("[GPTSoVITS_TTS] Cannot start GPT-SoVITS server.")
    #         return

    #     # if self.gpt_sovits_process is not None:#20260616_kpopmodder
    #     #     return

    #     # try:
    #     #     api_script = os.path.join(self.gpt_sovits_root, "api_v2.py")
    #     #     python_exe = os.path.join(
    #     #         self.gpt_sovits_root,
    #     #         "runtime",
    #     #         "python.exe"
    #     #     )

    #     #     self.gpt_sovits_process = subprocess.Popen(
    #     #         [
    #     #             python_exe,
    #     #             api_script,
    #     #             "-a",
    #     #             "127.0.0.1",
    #     #             "-p",
    #     #             "9880"
    #     #         ],
    #     #         cwd=self.gpt_sovits_root
    #     #     )

    #     if self.gpt_sovits_process is not None:#20260616_kpopmodder
    #         return

    #     if self.is_gpt_sovits_server_alive():#20260616_kpopmodder
    #         log_print(
    #             "[GPTSoVITS_TTS] Existing GPT-SoVITS API server detected. Reusing."
    #         )
    #         self.gpt_sovits_process = None
    #         return

    #     try:
    #         api_script = ...
    #         python_exe = ...

    #         log_print("[GPTSoVITS_TTS] Starting new GPT-SoVITS API server...")#20260616_kpopmodder

    #         self.gpt_sovits_process = subprocess.Popen(#20260616_kpopmodder
    #             [
    #                 python_exe,
    #                 api_script,
    #                 "-a",
    #                 "127.0.0.1",
    #                 "-p",
    #                 "9880"
    #             ],
    #             cwd=self.gpt_sovits_root
    #         )

    #         #log_print("[GPTSoVITS_TTS] GPT-SoVITS API server starting...")#20260616_kpopmodder
    #         if self.is_gpt_sovits_server_alive():#20260616_kpopmodder
    #             log_print("[GPTSoVITS_TTS] Existing GPT-SoVITS API server detected. Reusing.")
    #             return

    #         log_print("[GPTSoVITS_TTS] Starting new GPT-SoVITS API server...")#20260616_kpopmodder

    #         base_url = self.gpt_sovits_url.replace("/tts", "")

    #         for _ in range(20):
    #             try:
    #                 response = requests.get(
    #                     f"{base_url}/docs",
    #                     timeout=2
    #                 )

    #                 log_print(
    #                     f"[GPTSoVITS_TTS] server check: {response.status_code}"
    #                 )

    #                 if response.status_code == 200:
    #                     log_print("[GPTSoVITS_TTS] GPT-SoVITS API server ready.")
    #                     return

    #             except Exception as e:
    #                 log_print(f"[GPTSoVITS_TTS] Waiting server... {e}")
    #                 time.sleep(1)

    #         log_print("[GPTSoVITS_TTS] GPT-SoVITS API server start timeout.")

    #     except Exception as e:
    #         log_print(f"[GPTSoVITS_TTS] Failed to start server: {e}")

    def start_server(self):#20260616_kpopmodder
        self.server_manager.start_server(self.gpt_sovits_root)

    def update_model_list(self):
        self.model_manager.update_model_list()

    def load_weights(self):
        self.model_manager.load_weights()

    def synthesize_to_file(self, text, output_path):
        return self.api_client.synthesize_to_file(
            text=text,
            output_path=output_path,
            text_language=self.text_language,
            ref_audio_path=self.ref_audio_path,
            prompt_text=self.prompt_text,
            prompt_language=self.prompt_language,
            inference_options=self.inference_options()
        )

    def synthesize_to_bytes(self, text):
        return self.api_client.synthesize_to_bytes(
            text=text,
            text_language=self.text_language,
            ref_audio_path=self.ref_audio_path,
            prompt_text=self.prompt_text,
            prompt_language=self.prompt_language,
            inference_options=self.inference_options()
        )

    def set_inference_options(
        self,
        speed_factor=None,
        temperature=None,
        top_p=None,
        top_k=None,
        repetition_penalty=None
    ):
        if speed_factor is not None:
            self.speed_factor = float(speed_factor)
        if temperature is not None:
            self.temperature = float(temperature)
        if top_p is not None:
            self.top_p = float(top_p)
        if top_k is not None:
            self.top_k = int(top_k)
        if repetition_penalty is not None:
            self.repetition_penalty = float(repetition_penalty)

    def inference_options(self):
        return {
            "speed_factor": self.speed_factor,
            "top_k": self.top_k,
            "top_p": self.top_p,
            "temperature": self.temperature,
            "repetition_penalty": self.repetition_penalty,
        }

    def set_gpt_weight_by_name(self, name):
        self.model_manager.set_gpt_weight_by_name(name)

    def set_sovits_weight_by_name(self, name):
        self.model_manager.set_sovits_weight_by_name(name)

    def stop_server(self):
        self.server_manager.stop_server()

    def diagnostics(self):
        #20260717_kpopmodder: Diagnostics avoid HTTP probes unless process_manager.probe() is called explicitly.
        return {
            "api_url": self.gpt_sovits_url,
            "gpt_sovits_root_configured": bool(self.gpt_sovits_root),
            "process_running": self.process_manager.is_process_running(),
            "cuda_visible_devices": self.cuda_visible_devices,
            "config": self.config.to_dict(),
        }
