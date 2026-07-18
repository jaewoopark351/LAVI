# import io#20260616_kpopmodder
import os
# import uuid#20260616_kpopmodder

import gradio as gr
#from pydub import AudioSegment#20260616_kpopmodder

from core.logger import log_print
from plugin_system.interfaces import TTSPluginInterface
from plugins.GPTSoVITS.GPTSoVITS_TTS import GPTSoVITSTTS
#from plugins.GPTSoVITS.RVC_PostProcess import RVCPostProcess#20260617_kpopmodder

#20260620_kpopmodder: Import grouped GPTSoVITS helpers from gpt_sovits_core.
from plugins.GPTSoVITS.gpt_sovits_core.tts_synthesis_service import TTSSynthesisService#20260616_kpopmodder

from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_settings_controller import GPTSoVITSSettingsController#20260616_kpopmodder
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_tts_provider import GPTSoVITSTTSProvider

class GPTSoVITS(TTSPluginInterface):#20260615_kpopmodder
    #20260716_kpopmodder: P1-B representative metadata; discovery must not start the GPT-SoVITS server.
    PLUGIN_METADATA = {
        "id": "GPTSoVITS",
        "display_name": "GPT-SoVITS",
        "api_version": "1",
        "category": "text_to_speech",
        "entrypoint": "plugins.GPTSoVITS.GPTSoVITS:GPTSoVITS",
        "dependency_group": "Voice",
        "capabilities": [
            "text_to_speech",
            "gpt_sovits_api_server",
            "model_weight_switching",
        ],
        "config_schema": {
            "GPTSoVITS": {
                "gpt_sovits_root": "",
                "cuda_visible_devices": "1",
                "api_url": "http://127.0.0.1:9880/tts",
            },
        },
        "required_python_packages": ["requests"],
        "required_files": [
            "plugin:gpt_sovits_ckpt_dir",
            "plugin:gpt_sovits_model_dir",
        ],
        "required_executables": [],
        "required_services": ["GPT-SoVITS API server http://127.0.0.1:9880"],
        "supports_offline": True,
        "supports_cpu": False,
        "requires_gpu": True,
    }

    current_module_directory = os.path.dirname(__file__)

    def init(self):
        #20260717_kpopmodder: The plugin facade owns UI wiring; GPTSoVITSTTSProvider owns provider startup/shutdown.
        self.gpt_sovits_provider = GPTSoVITSTTSProvider(GPTSoVITSTTS())
        self.gpt_sovits = self.gpt_sovits_provider.runtime
        self.gpt_sovits_provider.init()

        #self.rvc = RVCPostProcess()#20260617_kpopmodder
        #self.rvc.init()#20260617_kpopmodder
        self.rvc = None#20260617_kpopmodder

        self.use_rvc = False

        self.synthesis_service = TTSSynthesisService(#20260616_kpopmodder
            current_module_directory=self.current_module_directory,
            gpt_sovits=self.gpt_sovits,
            rvc=self.rvc
        )

        self.settings_controller = GPTSoVITSSettingsController(
            gpt_sovits=self.gpt_sovits,
            rvc=None,
            get_use_rvc_callback=lambda: False,
            set_use_rvc_callback=self.set_use_rvc
        )

        # self.settings_controller = GPTSoVITSSettingsController(#20260617_kpopmodder
        #     gpt_sovits=self.gpt_sovits,
        #     rvc=self.rvc,
        #     get_use_rvc_callback=lambda: self.use_rvc,
        #     set_use_rvc_callback=self.set_use_rvc
        # )

    def create_ui(self):
        #with gr.Accordion(label="GPT-SoVITS + RVC Options", open=False):#20260617_kpopmodder
        with gr.Accordion(label="GPT-SoVITS Options", open=False):#20260617_kpopmodder
            gr.Markdown(
                # "GPT-SoVITS는 기본 TTS로 사용하고, "
                # "RVC는 선택 후처리로만 사용합니다."
                "GPT-SoVITS를 기본 TTS로 사용합니다. "
                "RVC 후처리는 제거되었습니다."
            )

            with gr.Row():
                self.gpt_sovits_url_input = gr.Textbox(
                    label="GPT-SoVITS API URL",
                    value=self.gpt_sovits.gpt_sovits_url
                )

            with gr.Row():
                self.gpt_ckpt_dropdown = gr.Dropdown(
                    label="GPT ckpt",
                    choices=self.gpt_sovits.gpt_ckpt_names,
                    value=(
                        self.gpt_sovits.gpt_ckpt_names[0]
                        if self.gpt_sovits.gpt_ckpt_names
                        else None
                    ),
                    interactive=True
                )

                self.sovits_model_dropdown = gr.Dropdown(
                    label="SoVITS pth",
                    choices=self.gpt_sovits.sovits_model_names,
                    value=(
                        self.gpt_sovits.sovits_model_names[0]
                        if self.gpt_sovits.sovits_model_names
                        else None
                    ),
                    interactive=True
                )

                self.gpt_sovits_refresh_button = gr.Button(
                    "Refresh GPTSoVITS"
                )

            with gr.Row():
                self.text_language_input = gr.Textbox(
                    label="Text Language",
                    value=self.gpt_sovits.text_language
                )

                self.prompt_language_input = gr.Textbox(
                    label="Prompt Language",
                    value=self.gpt_sovits.prompt_language
                )

            with gr.Row():
                self.ref_audio_path_input = gr.Textbox(
                    label="Reference Audio Path",
                    value=self.gpt_sovits.ref_audio_path,
                    placeholder="C:/path/to/reference.wav"
                )

            with gr.Row():
                self.prompt_text_input = gr.Textbox(
                    label="Prompt Text",
                    value=self.gpt_sovits.prompt_text,
                    lines=2
                )

            #gr.Markdown("### RVC Optional Post Process")#20260617_kpopmodder

            # with gr.Row():#20260617_kpopmodder
            #     self.use_rvc_checkbox = gr.Checkbox(
            #         label="Use RVC",
            #         value=self.use_rvc
            #     )

            #     self.rvc_model_dropdown = gr.Dropdown(
            #         label="RVC models",
            #         choices=self.rvc.rvc_model_names,
            #         value=(
            #             self.rvc.rvc_model_name
            #             if self.rvc.rvc_model_names
            #             else None
            #         ),
            #         interactive=True
            #     )

            #     self.rvc_refresh_button = gr.Button(
            #         "Refresh RVC",
            #         variant="primary"
            #     )

            # with gr.Row():
            #     self.download_model_input = gr.Textbox(
            #         label="RVC Model zip URL"
            #     )

            #     self.download_button = gr.Button(
            #         "Download RVC Model"
            #     )

            # with gr.Column():
            #     self.transpose_slider = gr.Slider(
            #         value=self.rvc.transpose,
            #         minimum=-24,
            #         maximum=24,
            #         step=1,
            #         label="Transpose"
            #     )

            #     self.index_rate_slider = gr.Slider(
            #         value=self.rvc.index_rate,
            #         minimum=0,
            #         maximum=1,
            #         step=0.01,
            #         label="Index Rate"
            #     )

            #     self.protect_slider = gr.Slider(
            #         value=self.rvc.protect,
            #         minimum=0,
            #         maximum=0.5,
            #         step=0.01,
            #         label="Protect"
            #     )

            self.gpt_sovits_url_input.change(
                #self.on_gpt_sovits_url_change,#20260616_kpopmodder
                self.settings_controller.on_gpt_sovits_url_change,#20260616_kpopmodder
                inputs=self.gpt_sovits_url_input,
                outputs=[]
            )

            self.gpt_ckpt_dropdown.input(
                #self.on_gpt_ckpt_change,#20260616_kpopmodder
                self.settings_controller.on_gpt_ckpt_change,#20260616_kpopmodder
                inputs=self.gpt_ckpt_dropdown,
                outputs=[]
            )

            self.sovits_model_dropdown.input(
                #self.on_sovits_model_change,#20260616_kpopomodder
                self.settings_controller.on_sovits_model_change,#20260616_kpopomodder
                inputs=self.sovits_model_dropdown,
                outputs=[]
            )

            self.gpt_sovits_refresh_button.click(
                #self.on_gpt_sovits_refresh,#20260616_kpopomodder
                self.settings_controller.on_gpt_sovits_refresh,#20260616_kpopomodder
                outputs=[
                    self.gpt_ckpt_dropdown,
                    self.sovits_model_dropdown
                ]
            )

            self.text_language_input.change(
                #self.on_text_language_change,#20260616_kpopomodder
                self.settings_controller.on_text_language_change,#20260616_kpopomodder
                inputs=self.text_language_input,
                outputs=[]
            )

            self.prompt_language_input.change(
                #self.on_prompt_language_change,#20260616_kpopomodder
                self.settings_controller.on_prompt_language_change,#20260616_kpopomodder
                inputs=self.prompt_language_input,
                outputs=[]
            )

            self.ref_audio_path_input.change(
                #self.on_ref_audio_path_change,#20260616_kpopomodder
                self.settings_controller.on_ref_audio_path_change,#20260616_kpopomodder
                inputs=self.ref_audio_path_input,
                outputs=[]
            )

            self.prompt_text_input.change(
                #self.on_prompt_text_change,#20260616_kpopomodder
                self.settings_controller.on_prompt_text_change,#20260616_kpopomodder
                inputs=self.prompt_text_input,
                outputs=[]
            )

            # self.use_rvc_checkbox.change(#20260617_kpopmodder
            #     #self.on_use_rvc_change,#20260616_kpopomodder
            #     self.settings_controller.on_use_rvc_change,#20260616_kpopomodder
            #     inputs=self.use_rvc_checkbox,
            #     outputs=[]
            # )

            # self.rvc_model_dropdown.input(
            #     #self.on_rvc_model_change,#20260616_kpopomodder
            #     self.settings_controller.on_rvc_model_change,#20260616_kpopomodder
            #     inputs=self.rvc_model_dropdown,
            #     outputs=[]
            # )

            # self.rvc_refresh_button.click(
            #     #self.on_rvc_refresh,#20260616_kpopomodder
            #     self.settings_controller.on_rvc_refresh,#20260616_kpopomodder
            #     outputs=[self.rvc_model_dropdown]
            # )

            # self.transpose_slider.change(
            #     #self.on_transpose_change,#20260616_kpopomodder
            #     self.settings_controller.on_transpose_change,#20260616_kpopomodder
            #     inputs=self.transpose_slider,
            #     outputs=[]
            # )

            # self.index_rate_slider.change(
            #     #self.on_index_rate_change,#20260616_kpopomodder
            #     self.settings_controller.on_index_rate_change,#20260616_kpopomodder
            #     inputs=self.index_rate_slider,
            #     outputs=[]
            # )

            # self.protect_slider.change(
            #     #self.on_protect_change,#20260616_kpopomodder
            #     self.settings_controller.on_protect_change,#20260616_kpopomodder
            #     inputs=self.protect_slider,
            #     outputs=[]
            # )

            # self.download_button.click(
            #     #self.download_model_from_url,#20260616_kpopomodder
            #     self.settings_controller.download_model_from_url,#20260616_kpopomodder
            #     inputs=self.download_model_input,
            #     outputs=[]
            # )

    def synthesize(self, text):#20260616_kpopmodder
        text = self.preprocess_text(text)

        if not text or not text.strip():
            return None

        return self.synthesis_service.synthesize(
            text=text,
            use_rvc=False
        )

    # def synthesize(self, text):
    #     text = self.preprocess_text(text)

    #     if not text or not text.strip():
    #         return None

    #     wav_filename = os.path.join(
    #         self.current_module_directory,
    #         f"gpt_sovits_output_{uuid.uuid4().hex}.wav"
    #     )

    #     try:
    #         self.gpt_sovits.synthesize_to_file(text, wav_filename)

    #         if self.use_rvc:
    #             audio_bytes = self.rvc.convert_file_to_bytes(wav_filename)

    #             if audio_bytes is not None:
    #                 return audio_bytes

    #             log_print(
    #                 "[GPTSoVITS] RVC failed. "
    #                 "Fallback to GPT-SoVITS output."
    #             )

    #         audio = AudioSegment.from_wav(wav_filename)
    #         buffer = io.BytesIO()
    #         audio.export(buffer, format="wav")

    #         return buffer.getvalue()

    #     except Exception as e:
    #         log_print(f"[GPTSoVITS] synthesize error: {e}")
    #         return None

    #     finally:
    #         try:
    #             if os.path.exists(wav_filename):
    #                 os.remove(wav_filename)
    #                 log_print(f"[GPTSoVITS] temp deleted: {wav_filename}")
    #         except Exception as e:
    #             log_print(f"[GPTSoVITS] temp delete failed: {e}")

    # def on_gpt_sovits_url_change(self, value):
    #     self.gpt_sovits.gpt_sovits_url = value.strip()

    # def on_gpt_ckpt_change(self, choice):
    #     self.gpt_sovits.set_gpt_weight_by_name(choice)

    # def on_sovits_model_change(self, choice):
    #     self.gpt_sovits.set_sovits_weight_by_name(choice)

    # def on_gpt_sovits_refresh(self):
    #     self.gpt_sovits.update_model_list()

    #     return (
    #         gr.update(
    #             choices=self.gpt_sovits.gpt_ckpt_names,
    #             value=(
    #                 os.path.basename(self.gpt_sovits.gpt_weight_path)
    #                 if self.gpt_sovits.gpt_weight_path
    #                 else None
    #             )
    #         ),
    #         gr.update(
    #             choices=self.gpt_sovits.sovits_model_names,
    #             value=(
    #                 os.path.basename(self.gpt_sovits.sovits_weight_path)
    #                 if self.gpt_sovits.sovits_weight_path
    #                 else None
    #             )
    #         )
    #     )

    # def on_text_language_change(self, value):
    #     self.gpt_sovits.text_language = value.strip() or "ko"

    # def on_prompt_language_change(self, value):
    #     self.gpt_sovits.prompt_language = value.strip() or "ko"

    # def on_ref_audio_path_change(self, value):
    #     self.gpt_sovits.ref_audio_path = value.strip()

    # def on_prompt_text_change(self, value):
    #     self.gpt_sovits.prompt_text = value.strip()

    # def on_use_rvc_change(self, use):
    #     self.use_rvc = use
    #     log_print(f"[GPTSoVITS] use_rvc={self.use_rvc}")

    # def on_rvc_model_change(self, choice):
    #     self.rvc.load_model(choice)

    # def on_rvc_refresh(self):
    #     self.rvc.update_model_list()

    #     return gr.update(
    #         choices=self.rvc.rvc_model_names,
    #         value=(
    #             self.rvc.rvc_model_names[0]
    #             if self.rvc.rvc_model_names
    #             else None
    #         )
    #     )

    # def on_transpose_change(self, value):
    #     self.rvc.set_transpose(value)

    # def on_index_rate_change(self, value):
    #     self.rvc.set_index_rate(value)

    # def on_protect_change(self, value):
    #     self.rvc.set_protect(value)

    # def download_model_from_url(self, url):
    #     self.rvc.download_model_from_url(url)

    def preprocess_text(self, text):
        text = text.strip()

        if not text:
            return ""

        text = text.replace("\n\n", "\n")
        text = text.replace("😊", "")
        text = text.replace("😂", "")
        text = text.replace("❤", "")

        return text

    def __del__(self):
        try:
            if hasattr(self, "gpt_sovits_provider"):
                self.gpt_sovits_provider.shutdown()
            elif hasattr(self, "gpt_sovits"):
                self.gpt_sovits.stop_server()
        except Exception:
            pass

    def cleanup(self):
        try:
            if hasattr(self, "gpt_sovits_provider"):
                self.gpt_sovits_provider.shutdown()
            elif hasattr(self, "gpt_sovits"):
                self.gpt_sovits.stop_server()
        except Exception as e:
            log_print(f"[GPTSoVITS] cleanup failed: {e}")

    def stop(self):
        #20260717_kpopmodder: Provider lifecycle must release the GPT-SoVITS child process before shutdown.
        self.cleanup()

    def shutdown(self):
        #20260717_kpopmodder: Keep shutdown idempotent for RuntimeLifecycle and PluginSelection cleanup paths.
        self.cleanup()

    def diagnostics(self):
        diagnostics = super().diagnostics()
        provider = getattr(self, "gpt_sovits_provider", None)
        if provider is not None:
            diagnostics["provider"] = provider.diagnostics()
        return diagnostics

    # def set_use_rvc(self, value):#20260616_kpopmodder
    #     self.use_rvc = value

    def set_use_rvc(self, value):#20260617_kpopmodder
        self.use_rvc = False
        log_print("[GPTSoVITS] RVC removed. use_rvc=False")

# import io
# import os
# import shutil
# import json#20260613_kpopmodder
# import uuid#20260614_kpopmodder
# import time
# import requests
# import gradio as gr
# import soundfile as sf
# import subprocess#20260612_kpopmodder

# from pydub import AudioSegment

# from LAV_utils import download_and_extract_zip
# from plugin_system.interfaces import TTSPluginInterface

# from plugins.rvc.inferrvc import load_torchaudio
# from plugins.rvc.inferrvc import RVC
# from core.logger import log_print, debug_print#20260612_kpopmodder


# class GPTSoVITS(TTSPluginInterface):#20260611_kpopmodder
#     current_module_directory = os.path.dirname(__file__)

#     gpt_sovits_config_dir = os.path.join(#20260613_kpopmodder
#         current_module_directory,
#         "config"
#     )

#     gpt_sovits_config_path = os.path.join(#20260613_kpopmodder
#         gpt_sovits_config_dir,
#         "gpt_sovits_config.json"
#     )

#     default_gpt_sovits_config = {#20260613_kpopmodder
#         "gpt_sovits_root": r"C:\Vtuber_Souorce_Code\GPT-SoVITS-v2pro-20250604-nvidia50",
#         "show_install_warning": True
#     }

#     GPT_SOVITS_OUTPUT_FILENAME = os.path.join(
#         current_module_directory, "gpt_sovits_output.wav"
#     )
#     RVC_OUTPUT_FILENAME = os.path.join(
#         current_module_directory, "rvc_output.wav"
#     )

#     rvc_model_dir = os.path.join(current_module_directory, "rvc_model_dir")
#     rvc_index_dir = os.path.join(current_module_directory, "rvc_index_dir")

#     gpt_sovits_ckpt_dir = os.path.join(#20260612_kpopmodder
#         current_module_directory,
#         "gpt_sovits_ckpt_dir"
#     )

#     gpt_sovits_model_dir = os.path.join(#20260612_kpopmodder
#         current_module_directory,
#         "gpt_sovits_model_dir"
#     )

#     gpt_weight_path = ""#20260612_kpopmodder
#     sovits_weight_path = ""#20260612_kpopmodder

#     gpt_ckpt_names = []#20260612_kpopmodder
#     sovits_model_names = []#20260612_kpopmodder

#     # GPT-SoVITS API Server
#     gpt_sovits_url = "http://127.0.0.1:9880/tts"

#     # GPT-SoVITS trained model paths
# #    gpt_weight_path = r"C:\Vtuber_Souorce_Code\GPT-SoVITS-v2pro-20250604-nvidia50\GPT_weights_v2Pro\project1-e50.ckpt"#20260612_kpopmodder#GPT-SoVITS API 서버 안의 모델이 이 경로에서 실행된다고 가정, 경로 오타 조심
# #    sovits_weight_path = r"C:\Vtuber_Souorce_Code\GPT-SoVITS-v2pro-20250604-nvidia50\SoVITS_weights_v2Pro\project1_e24_s4512.pth"#20260612_kpopmodder#GPT-SoVITS API 서버 안의 모델이 이 경로에서 실행된다고 가정, 경로 오타 조심
    
# #    gpt_sovits_root = r"C:\Vtuber_Souorce_Code\GPT-SoVITS-v2pro-20250604-nvidia50"#20260612_kpopmodder#GPT-SoVITS 설치 경로, 경로 오타 조심!!!!
#     gpt_sovits_root = ""#20260613_kpopmodder
#     gpt_sovits_process = None#20260612_kpopmodder

#     # GPT-SoVITS parameters
#     text_language = "ko"
#     prompt_language = "ko"
#     prompt_text = "진짜 이때만큼 흥분했던 기억이 없는 것 같아요. 이 방송 이후로 1년이 지났지만 여전히 최고의 기억입니다."
# #    ref_audio_path = r"C:\Vtuber_Souorce_Code\LAVI\voices\ref.wav"#20260611_kpopmodder
#     base_dir = os.path.abspath(#20260612_kpopmodder
#         os.path.join(os.path.dirname(__file__), "..", "..")
#     )

#     ref_audio_path = os.path.join(#20260612_kpopmodder
#         base_dir,
#         "voices",
#         "ref.wav"
#     )

#     # RVC parameters
#     rvc_model_name = ""
#     use_rvc = False#20260613_kpopmodder#RVC on/off bool
#     transpose = 0
#     index_rate = 0.75
#     protect = 0.5

#     def init(self):
#         self.load_gpt_sovits_config()#20260613_kpopmodder

#         os.makedirs(self.rvc_model_dir, exist_ok=True)
#         os.makedirs(self.rvc_index_dir, exist_ok=True)
#         os.makedirs(self.gpt_sovits_ckpt_dir, exist_ok=True)#20260612_kpopmodder
#         os.makedirs(self.gpt_sovits_model_dir, exist_ok=True)#20260612_kpopmodder 

#         os.environ["RVC_MODELDIR"] = self.rvc_model_dir
#         os.environ["RVC_INDEXDIR"] = self.rvc_index_dir
#         os.environ["RVC_OUTPUTFREQ"] = "44100"
#         os.environ["RVC_RETURNBLOCKING"] = "False"

#         self.update_gpt_sovits_model_list()#20260612_kpopmodder
#         self.start_gpt_sovits_server()#20260612_kpopmodder
#         self.load_gpt_sovits_weights()#20260612_kpopmodder

#         self.model = None
#         self.update_rvc_model_list()

#         if len(self.rvc_model_names) > 0:
#             self.rvc_model_name = self.rvc_model_names[0]
#             self.model = RVC(self.rvc_model_name)

#     def create_ui(self):
#         with gr.Accordion(label="GPT-SoVITS + RVC Options", open=False):
#             gr.Markdown("GPT-SoVITS API 서버를 먼저 실행해야 합니다. 기본 주소: http://127.0.0.1:9880")

#             with gr.Row():
#                 self.gpt_sovits_url_input = gr.Textbox(
#                     label="GPT-SoVITS API URL",
#                     value=self.gpt_sovits_url,
#                 )

#             with gr.Row():#20260612_kpopmodder
#                 self.gpt_ckpt_dropdown = gr.Dropdown(
#                     label="GPT ckpt",
#                     choices=self.gpt_ckpt_names,
#                     value=self.gpt_ckpt_names[0] if self.gpt_ckpt_names else None,
#                     interactive=True,
#                 )

#                 self.sovits_model_dropdown = gr.Dropdown(#20260612_kpopmodder
#                     label="SoVITS pth",
#                     choices=self.sovits_model_names,
#                     value=self.sovits_model_names[0] if self.sovits_model_names else None,
#                     interactive=True,
#                 )

#                 self.gpt_sovits_refresh_button = gr.Button(#20260612_kpopmodder
#                     "Refresh GPTSoVITS"
#                 )

#             with gr.Row():
#                 self.text_language_input = gr.Textbox(
#                     label="Text Language",
#                     value=self.text_language,
#                 )
#                 self.prompt_language_input = gr.Textbox(
#                     label="Prompt Language",
#                     value=self.prompt_language,
#                 )

#             with gr.Row():
#                 self.ref_audio_path_input = gr.Textbox(
#                     label="Reference Audio Path",
#                     value=self.ref_audio_path,
#                     placeholder="C:/path/to/reference.wav",
#                 )

#             with gr.Row():
#                 self.prompt_text_input = gr.Textbox(
#                     label="Prompt Text",
#                     value=self.prompt_text,
#                     lines=2,
#                 )

#             with gr.Row():
#                 self.use_rvc_checkbox = gr.Checkbox(
#                     label="Use RVC",
#                     value=self.use_rvc,
#                 )
#                 self.rvc_model_dropdown = gr.Dropdown(
#                     label="RVC models",
#                     choices=self.rvc_model_names,
#                     value=self.rvc_model_name if len(self.rvc_model_names) > 0 else None,
#                     interactive=True,
#                 )
#                 self.refresh_button = gr.Button("Refresh", variant="primary")

#             with gr.Row():
#                 self.download_model_input = gr.Textbox(label="RVC Model zip URL")
#                 self.download_button = gr.Button("Download RVC Model")

#             with gr.Column():
#                 self.transpose_slider = gr.Slider(
#                     value=self.transpose,
#                     minimum=-24,
#                     maximum=24,
#                     step=1,
#                     label="Transpose",
#                 )
#                 self.index_rate_slider = gr.Slider(
#                     value=self.index_rate,
#                     minimum=0,
#                     maximum=1,
#                     step=0.01,
#                     label="Index Rate",
#                 )
#                 self.protect_slider = gr.Slider(
#                     value=self.protect,
#                     minimum=0,
#                     maximum=0.5,
#                     step=0.01,
#                     label="Protect",
#                 )

#         self.gpt_ckpt_dropdown.input(#20260612_kpopmodder
#             self.on_gpt_ckpt_change,
#             inputs=self.gpt_ckpt_dropdown,
#             outputs=[],
#         )

#         self.sovits_model_dropdown.input(#20260612_kpopmodder
#             self.on_sovits_model_change,
#             inputs=self.sovits_model_dropdown,
#             outputs=[],
#         )

#         self.gpt_sovits_refresh_button.click(#20260612_kpopmodder
#             self.on_gpt_sovits_refresh,
#             outputs=[
#                 self.gpt_ckpt_dropdown,
#                 self.sovits_model_dropdown
#             ],
#         )

#         self.gpt_sovits_url_input.change(
#             self.on_gpt_sovits_url_change,
#             inputs=self.gpt_sovits_url_input,
#             outputs=[],
#         )
#         self.text_language_input.change(
#             self.on_text_language_change,
#             inputs=self.text_language_input,
#             outputs=[],
#         )
#         self.prompt_language_input.change(
#             self.on_prompt_language_change,
#             inputs=self.prompt_language_input,
#             outputs=[],
#         )
#         self.ref_audio_path_input.change(
#             self.on_ref_audio_path_change,
#             inputs=self.ref_audio_path_input,
#             outputs=[],
#         )
#         self.prompt_text_input.change(
#             self.on_prompt_text_change,
#             inputs=self.prompt_text_input,
#             outputs=[],
#         )

#         self.use_rvc_checkbox.change(
#             self.on_use_rvc_click,
#             inputs=self.use_rvc_checkbox,
#             outputs=[],
#         )
#         self.rvc_model_dropdown.input(
#             self.on_rvc_model_change,
#             inputs=self.rvc_model_dropdown,
#             outputs=[],
#         )
#         self.refresh_button.click(
#             self.on_refresh,
#             outputs=[self.rvc_model_dropdown],
#         )

#         self.transpose_slider.change(
#             self.on_transpose_change,
#             inputs=self.transpose_slider,
#             outputs=[],
#         )
#         self.index_rate_slider.change(
#             self.on_index_rate_change,
#             inputs=self.index_rate_slider,
#             outputs=[],
#         )
#         self.protect_slider.change(
#             self.on_protect_change,
#             inputs=self.protect_slider,
#             outputs=[],
#         )
#         self.download_button.click(
#             self.download_model_from_url,
#             inputs=self.download_model_input,
#             outputs=[],
#         )

#     def synthesize(self, text):#20260615_kpopmodder
#         text = self.preprocess_text(text)

#         wav_filename = os.path.join(
#             self.current_module_directory,
#             f"gpt_sovits_output_{uuid.uuid4().hex}.wav"
#         )

#         rvc_filename = os.path.join(
#             self.current_module_directory,
#             f"rvc_output_{uuid.uuid4().hex}.wav"
#         )

#         try:
#             if not text.strip():
#                 return None

#             self.call_gpt_sovits(text, wav_filename)

#             if not self.use_rvc:
#                 audio = AudioSegment.from_wav(wav_filename)
#                 buffer = io.BytesIO()
#                 audio.export(buffer, format="wav")
#                 return buffer.getvalue()

#             if self.model is None:
#                 log_print("[GPTSoVITS] RVC model is not loaded.")
#                 return None

#             start_time = time.time()
#             aud, sr = load_torchaudio(wav_filename)
#             log_print(f"load_torchaudio: {time.time() - start_time:.5f} seconds")

#             # 너무 짧은 음성은 RVC가 padding 오류를 내므로 RVC를 건너뜀#20260612_kpopmodder
#             if aud.shape[-1] <= 32000:#20260612_kpopmodder
#                 log_print(
#                     f"[GPTSoVITS] Audio too short for RVC. "
#                     f"Skipping RVC. samples={aud.shape[-1]}"
#                 )

#                 audio = AudioSegment.from_wav(wav_filename)
#                 buffer = io.BytesIO()
#                 audio.export(buffer, format="wav")
#                 return buffer.getvalue()

#             start_time = time.time()
#             converted_audio = self.model(
#                 aud,
#                 f0_up_key=self.transpose,
#                 output_volume=RVC.MATCH_ORIGINAL,
#                 index_rate=self.index_rate,
#                 protect=self.protect,
#             )
#             log_print(f"RVC model processing: {time.time() - start_time:.5f} seconds")

#             converted_audio_cpu = converted_audio.cpu().numpy()
#             sf.write(rvc_filename, converted_audio_cpu, 44100)

#             audio = AudioSegment.from_wav(rvc_filename)
#             buffer = io.BytesIO()
#             audio.export(buffer, format="wav")
#             return buffer.getvalue()

#         except Exception as e:
#             log_print(f"[GPTSoVITS] synthesize error: {e}")
#             return None

#         finally:
#             for temp_file in [wav_filename, rvc_filename]:
#                 try:
#                     if os.path.exists(temp_file):
#                         os.remove(temp_file)
#                         log_print(f"[GPTSoVITS] temp deleted: {temp_file}")
#                 except Exception as e:
#                     log_print(f"[GPTSoVITS] temp delete failed: {e}")

#     # def synthesize(self, text):
#     #     text = self.preprocess_text(text)

#     #     try:
#     #         if not text.strip():#20260612_kpopmodder
#     #             return None

#     #         wav_filename = os.path.join(#20260614_kpopmodder
#     #             self.current_module_directory,
#     #             f"gpt_sovits_output_{uuid.uuid4().hex}.wav"
#     #         )

#     #         rvc_filename = os.path.join(#20260614_kpopmodder
#     #             self.current_module_directory,
#     #             f"rvc_output_{uuid.uuid4().hex}.wav"
#     #         )

#     #         #self.call_gpt_sovits(text)#20260614_kpopmodder
#     #         self.call_gpt_sovits(text, wav_filename)#20260614_kpopmodder
#     #     except Exception as e:
#     #         log_print(f"[GPTSoVITS] GPT-SoVITS TTS error: {e}")#20260612_kpopmodder
#     #         return None

#     #     #wav_filename = self.GPT_SOVITS_OUTPUT_FILENAME#20260614_kpopmodder

#     #     if not self.use_rvc:
#     #         audio = AudioSegment.from_wav(wav_filename)
#     #         buffer = io.BytesIO()
#     #         audio.export(buffer, format="wav")
#     #         return buffer.getvalue()

#     #     if self.model is None:
#     #         log_print("[GPTSoVITS] RVC model is not loaded.")#20260612_kpopmodder
#     #         return None

#     #     try:
#     #         start_time = time.time()
#     #         aud, sr = load_torchaudio(wav_filename)
#     #         log_print(f"load_torchaudio: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#     #         # 너무 짧은 음성은 RVC가 padding 오류를 내므로 RVC를 건너뜀#20260612_kpopmodder
#     #         if aud.shape[-1] <= 32000:#20260612_kpopmodder
#     #             log_print(
#     #                 f"[GPTSoVITS] Audio too short for RVC. "
#     #                 f"Skipping RVC. samples={aud.shape[-1]}"#20260612_kpopmodder
#     #             )

#     #             audio = AudioSegment.from_wav(wav_filename)
#     #             buffer = io.BytesIO()
#     #             audio.export(buffer, format="wav")
#     #             return buffer.getvalue()#20260612_kpopmodder

#     #         start_time = time.time()
#     #         converted_audio = self.model(
#     #             aud,
#     #             f0_up_key=self.transpose,
#     #             output_volume=RVC.MATCH_ORIGINAL,
#     #             index_rate=self.index_rate,
#     #             protect=self.protect,
#     #         )
#     #         log_print(f"RVC model processing: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#     #         converted_audio_cpu = converted_audio.cpu().numpy()
#     #         # sf.write(self.RVC_OUTPUT_FILENAME, converted_audio_cpu, 44100)#20260614_kpopmodder

#     #         # audio = AudioSegment.from_wav(self.RVC_OUTPUT_FILENAME)#20260614_kpopmodder

#     #         sf.write(rvc_filename, converted_audio_cpu, 44100)#20260614_kpopmodder

#     #         audio = AudioSegment.from_wav(rvc_filename)#20260614_kpopmodder

#     #         buffer = io.BytesIO()
#     #         audio.export(buffer, format="wav")
#     #         return buffer.getvalue()

#     #     except Exception as e:
#     #         log_print(f"[GPTSoVITS] RVC conversion error: {e}")#20260612_kpopmodder
#     #         return None

#     def load_gpt_sovits_config(self):#20260613_kpopmodder
#         os.makedirs(self.gpt_sovits_config_dir, exist_ok=True)

#         if not os.path.exists(self.gpt_sovits_config_path):
#             with open(self.gpt_sovits_config_path, "w", encoding="utf-8") as f:
#                 json.dump(
#                     self.default_gpt_sovits_config,
#                     f,
#                     ensure_ascii=False,
#                     indent=4
#                 )

#             self.gpt_sovits_root = self.default_gpt_sovits_config["gpt_sovits_root"]

#             log_print(
#                 f"[GPTSoVITS] Config created: {self.gpt_sovits_config_path}"
#             )

#             self.check_gpt_sovits_install()#20260613_kpopmodder
#             return

#         try:
#             with open(self.gpt_sovits_config_path, "r", encoding="utf-8") as f:
#                 config = json.load(f)

#             self.gpt_sovits_root = config.get("gpt_sovits_root", "").strip()

#         except Exception as e:
#             log_print(f"[GPTSoVITS] Config load failed: {e}")
#             self.gpt_sovits_root = self.default_gpt_sovits_config["gpt_sovits_root"]

#         self.check_gpt_sovits_install()

#     def check_gpt_sovits_install(self):#20260613_kpopmodder
#         if not self.gpt_sovits_root:
#             log_print("[GPTSoVITS] GPT-SoVITS root path is empty.")
#             return False

#         if not os.path.exists(self.gpt_sovits_root):
#             log_print(
#                 "[GPTSoVITS] GPT-SoVITS가 설치되어 있지 않거나 경로가 잘못되었습니다.\n"
#                 f"[GPTSoVITS] 현재 설정 경로: {self.gpt_sovits_root}\n"
#                 f"[GPTSoVITS] 설정 파일을 수정하세요: {self.gpt_sovits_config_path}"
#             )
#             return False

#         api_script = os.path.join(self.gpt_sovits_root, "api_v2.py")
#         python_exe = os.path.join(self.gpt_sovits_root, "runtime", "python.exe")

#         if not os.path.exists(api_script):
#             log_print(f"[GPTSoVITS] api_v2.py not found: {api_script}")
#             return False

#         if not os.path.exists(python_exe):
#             log_print(f"[GPTSoVITS] runtime python not found: {python_exe}")
#             return False

#         log_print(f"[GPTSoVITS] GPT-SoVITS root OK: {self.gpt_sovits_root}")
#         return True

#     def start_gpt_sovits_server(self):#20260612_kpopmodder
#         if not self.check_gpt_sovits_install():#20260613_kpopmodder
#             log_print("[GPTSoVITS] GPT-SoVITS 서버를 시작할 수 없습니다. 설치 경로를 확인하세요.")
#             return
    
#         try:
#             if self.gpt_sovits_process is None:
#                 api_script = os.path.join(self.gpt_sovits_root, "api_v2.py")
#                 python_exe = os.path.join(self.gpt_sovits_root, "runtime", "python.exe")

#                 self.gpt_sovits_process = subprocess.Popen(
#                     [python_exe, api_script, "-a", "127.0.0.1", "-p", "9880"],
#                     cwd=self.gpt_sovits_root,
#                 )

#                 log_print("[GPTSoVITS] GPT-SoVITS API server starting...")
                
#                 base_url = self.gpt_sovits_url.replace("/tts", "")#20260612_kpopmodder

# #                time.sleep(10)
#                 for _ in range(20):#20260612_kpopmodder
#                     try:
#                         # response = requests.get("http://127.0.0.1:9880")
#                         # if response.status_code == 200:
#                         #     log_print("[GPTSoVITS] GPT-SoVITS API server ready.")
#                         #     return
#                         response = requests.get(#20260612_kpopmodder
#                             f"{base_url}/docs",
#                             timeout=2
#                         )

#                         log_print(
#                             f"[GPTSoVITS] GPT-SoVITS server check: {response.status_code}"
#                         )
#                         log_print("[GPTSoVITS] GPT-SoVITS API server ready.")
#                         return#20260612_kpopmodder

#                     except Exception as e:#20260612_kpopmodder
#                         log_print(f"[GPTSoVITS] Waiting server... {e}")#20260612_kpopmodder

#                     time.sleep(1)

#                 log_print("[GPTSoVITS] GPT-SoVITS API server start timeout.")#20260612_kpopmodder

#         except Exception as e:
#             log_print(f"[GPTSoVITS] Failed to start GPT-SoVITS server: {e}")

#     def load_gpt_sovits_weights(self):#20260612_kpopmodder
#         try:
#             base_url = self.gpt_sovits_url.replace("/tts", "")

#             if self.gpt_weight_path and os.path.exists(self.gpt_weight_path):
#                 response = requests.get(
#                     f"{base_url}/set_gpt_weights",
#                     params={"weights_path": self.gpt_weight_path},
#                     timeout=60,
#                 )
#                 log_print(
#                     f"[GPTSoVITS] GPT weight load: {response.status_code} {self.gpt_weight_path}"
#                 )
#             else:
#                 log_print(f"[GPTSoVITS] GPT weight not found: {self.gpt_weight_path}")

#             if self.sovits_weight_path and os.path.exists(self.sovits_weight_path):
#                 response = requests.get(
#                     f"{base_url}/set_sovits_weights",
#                     params={"weights_path": self.sovits_weight_path},
#                     timeout=60,
#                 )
#                 log_print(
#                     f"[GPTSoVITS] SoVITS weight load: {response.status_code} {self.sovits_weight_path}"
#                 )
#             else:
#                 log_print(f"[GPTSoVITS] SoVITS weight not found: {self.sovits_weight_path}")

#         except Exception as e:
#             log_print(f"[GPTSoVITS] Weight auto-load failed: {e}")

#     #def call_gpt_sovits(self, text):#20260614_kpopmodder
#     def call_gpt_sovits(self, text, output_path):#20260614_kpopmodder
#         text = text.strip()#20260612_kpopmodder

#         if not text:#20260612_kpopmodder
#             log_print("[GPTSoVITS] Empty text. Skipping GPT-SoVITS.")
#             return

#         if not self.ref_audio_path:
#             raise ValueError("Reference Audio Path가 비어 있습니다.")

#         if not os.path.exists(self.ref_audio_path):
#             raise FileNotFoundError(f"Reference audio not found: {self.ref_audio_path}")

#         params = {
#             "text": text,
#             "text_lang": self.text_language,
#             "ref_audio_path": self.ref_audio_path,
#             "prompt_text": self.prompt_text,
#             "prompt_lang": self.prompt_language,
#             "media_type": "wav",
#             "streaming_mode": "false",
#         }

#         log_print(f"[GPTSoVITS] Requesting GPT-SoVITS: {self.gpt_sovits_url}")#20260612_kpopmodder
#         response = requests.get(self.gpt_sovits_url, params=params, timeout=120)

#         if response.status_code != 200:
#             raise RuntimeError(
#                 f"GPT-SoVITS API failed: {response.status_code} {response.text}"
#             )

#         # with open(self.GPT_SOVITS_OUTPUT_FILENAME, "wb") as f:#20260614_kpopmodder
#         #     f.write(response.content)

#         # log_print(f"[GPTSoVITS] Output saved: {self.GPT_SOVITS_OUTPUT_FILENAME}")#20260612_kpopmodder

#         with open(output_path, "wb") as f:#20260614_kpopmodder
#             f.write(response.content)

#         log_print(f"[GPTSoVITS] Output saved: {output_path}")#20260614_kpopmodder

#     def update_gpt_sovits_model_list(self):#20260612_kpopmodder
#         self.gpt_ckpt_names = []
#         self.sovits_model_names = []

#         for name in os.listdir(self.gpt_sovits_ckpt_dir):
#             if name.endswith(".ckpt"):
#                 self.gpt_ckpt_names.append(name)

#         for name in os.listdir(self.gpt_sovits_model_dir):
#             if name.endswith(".pth"):
#                 self.sovits_model_names.append(name)

#         if self.gpt_ckpt_names:
#             self.gpt_weight_path = os.path.join(
#                 self.gpt_sovits_ckpt_dir,
#                 self.gpt_ckpt_names[0]
#             )

#         if self.sovits_model_names:
#             self.sovits_weight_path = os.path.join(
#                 self.gpt_sovits_model_dir,
#                 self.sovits_model_names[0]
#             )

#     def update_rvc_model_list(self):
#         self.rvc_model_names = []

#         if not os.path.exists(self.rvc_model_dir):
#             os.makedirs(self.rvc_model_dir, exist_ok=True)

#         for name in os.listdir(self.rvc_model_dir):
#             if name.endswith(".pth"):
#                 self.rvc_model_names.append(name)

#     def download_model_from_url(self, url):
#         if not url:
#             log_print("[GPTSoVITS] Empty model URL.")#20260612_kpopmodder
#             return

#         folder_path = download_and_extract_zip(
#             url,
#             extract_to=self.current_module_directory,
#         )

#         pth_file_path = None
#         base_name = None

#         for file in os.listdir(folder_path):
#             if file.endswith(".pth"):
#                 base_name = os.path.splitext(file)[0]
#                 pth_file_path = os.path.join(folder_path, file)
#                 break

#         if not pth_file_path or not base_name:
#             log_print("No .pth file found in the folder.")#20260612_kpopmodder
#             return

#         index_file_path = None

#         for file in os.listdir(folder_path):
#             if file.endswith(".index"):
#                 index_file_path = os.path.join(folder_path, file)
#                 break

#         shutil.move(
#             pth_file_path,
#             os.path.join(self.rvc_model_dir, os.path.basename(pth_file_path)),
#         )

#         if index_file_path:
#             new_index_name = base_name + ".index"
#             new_index_path = os.path.join(folder_path, new_index_name)

#             if os.path.basename(index_file_path) != new_index_name:
#                 os.rename(index_file_path, new_index_path)
#                 index_file_path = new_index_path

#             shutil.move(
#                 index_file_path,
#                 os.path.join(self.rvc_index_dir, os.path.basename(index_file_path)),
#             )
#         else:
#             log_print(f"No .index file found for {base_name}")#20260612_kpopmodder

#         try:
#             shutil.rmtree(folder_path)
#         except Exception:
#             pass

#         self.update_rvc_model_list()

#     def on_gpt_sovits_url_change(self, value):
#         self.gpt_sovits_url = value.strip()

#     def on_text_language_change(self, value):
#         self.text_language = value.strip() or "ko"

#     def on_prompt_language_change(self, value):
#         self.prompt_language = value.strip() or "ko"

#     def on_ref_audio_path_change(self, value):
#         self.ref_audio_path = value.strip()

#     def on_prompt_text_change(self, value):
#         self.prompt_text = value.strip()

#     def on_use_rvc_click(self, use):
#         self.use_rvc = use

#     def on_gpt_ckpt_change(self, choice):
#         self.gpt_weight_path = os.path.join(
#             self.gpt_sovits_ckpt_dir,
#             choice
#         )
#         self.load_gpt_sovits_weights()

#     def on_sovits_model_change(self, choice):#20260612_kpopmodder
#         self.sovits_weight_path = os.path.join(
#             self.gpt_sovits_model_dir,
#             choice
#         )
#         self.load_gpt_sovits_weights()

#     def on_gpt_sovits_refresh(self):#20260612_kpopmodder
#         self.update_gpt_sovits_model_list()

#         return (
# #            gr.update(choices=self.gpt_ckpt_names),
# #            gr.update(choices=self.sovits_model_names),
#             gr.update(#20260612_kpopmodder
#                 choices=self.gpt_ckpt_names,
#                 value=self.gpt_ckpt_names[0] if self.gpt_ckpt_names else None
#             ),
#             gr.update(#20260612_kpopmodder
#                 choices=self.sovits_model_names,
#                 value=self.sovits_model_names[0] if self.sovits_model_names else None
#             ),
#         )

#     def on_rvc_model_change(self, choice):
#         self.rvc_model_name = choice

#         if self.rvc_model_name:
#             self.model = RVC(self.rvc_model_name)

#     def on_refresh(self):
#         self.update_rvc_model_list()
#         return gr.update(choices=self.rvc_model_names)

#     def on_transpose_change(self, value):
#         self.transpose = value

#     def on_index_rate_change(self, value):
#         self.index_rate = value

#     def on_protect_change(self, value):
#         self.protect = value

#     def preprocess_text(self, text):
#         return text
    
#     def __del__(self):#20260612_kpopmodder
#         if self.gpt_sovits_process:
#             log_print("[GPTSoVITS] Terminating GPT-SoVITS server...")
#             self.gpt_sovits_process.terminate()
