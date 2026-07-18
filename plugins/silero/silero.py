import os
import sys
import time
import gradio as gr
import requests
from plugin_system.interfaces import TTSPluginInterface
from core.logger import log_print#20260612_kpopmodder
from core.process import launch_process

# speaker bookmarks: en_18, en_21, en_37, en_39, en_43, en_72


class Silero(TTSPluginInterface):
    PLUGIN_METADATA = {
        "id": "Silero",
        "display_name": "Silero TTS",
        "api_version": "1",
        "category": "text_to_speech",
        "entrypoint": "plugins.silero.silero:Silero",
        "dependency_group": "Voice",
        "capabilities": ("text_to_speech", "silero_api_server"),
        "required_python_packages": ("requests",),
        "required_files": (),
        "required_executables": (),
        "required_services": ("Silero API server http://127.0.0.1:8435",),
        "supports_offline": True,
        "supports_cpu": True,
    }

    silero_server_started = False
    SILERO_URL_LOCAL = "127.0.0.1"
    PORT = "8435"
    current_module_directory = os.path.dirname(__file__)
    session_path = os.path.join(
        current_module_directory, "session")
    VOICE_OUTPUT_FILENAME = os.path.join(
        current_module_directory, "synthesized_voice.wav")

    current_language = None
    current_speaker = None

    def init(self):
        log_print("initializing silero...")#20260612_kpopmodder
        self.start_silero_server()
        self.init_session(self.session_path)

    def synthesize(self, text):
        url = f"http://{self.SILERO_URL_LOCAL}:{self.PORT}/tts/generate"

        data = {
            "speaker": self.current_speaker,
            "text": text,
            "session": ""
        }
        log_print(data)#20260612_kpopmodder
        AudioResponse = requests.request("POST", url, json=data)

        with open(self.VOICE_OUTPUT_FILENAME, "wb") as file:
            file.write(AudioResponse.content)
        return AudioResponse.content

    def create_ui(self):
        language_names = self.get_langauges()
        speaker_names = self.get_speaker_names()
        self.current_speaker = speaker_names[0]
        with gr.Accordion(label="Silero Options", open=False):
            with gr.Row():
                self.language_dropdown = gr.Dropdown(
                    choices=language_names,
                    value="v3_en.pt",
                    label="Language: "
                )
                self.speaker_dropdown = gr.Dropdown(
                    choices=speaker_names,
                    value="en_18",
                    label="Speaker: "
                )

                self.language_dropdown.input(self.on_language_change, inputs=[
                    self.language_dropdown], outputs=[self.speaker_dropdown])
                self.speaker_dropdown.input(
                    self.on_speaker_change, inputs=[self.speaker_dropdown])

    def start_silero_server(self):
        if (self.silero_server_started):
            return

        # start silero server
        command = [sys.executable, "-m", "silero_api_server", "-p", str(self.PORT)]
        launch_process(command)
        self.silero_server_started = True

    def init_session(self, session_path):
        url = f"http://{self.SILERO_URL_LOCAL}:{self.PORT}/tts/session"
        while True:
            try:
                data = {
                    "path": session_path
                }
                response = requests.request("POST", url, json=data)
                break
            except Exception:
                log_print("Waiting for silero to start... ")#20260612_kpopmodder
                time.sleep(0.5)
        log_print("session init result")#20260612_kpopmodder
        log_print(response.text)#20260612_kpopmodder

    def get_langauges(self):
        url = f"http://{self.SILERO_URL_LOCAL}:{self.PORT}/tts/language"
        response = requests.request("GET", url)

        return response.json()

    def get_speakers(self):
        url = f"http://{self.SILERO_URL_LOCAL}:{self.PORT}/tts/speakers"
        response = requests.request("GET", url)
        return response.json()

    def get_speaker_names(self):
        return [speaker['name'] for speaker in self.get_speakers()]

    def on_language_change(self, choice):
        # update speakers dropdown
        url = f"http://{self.SILERO_URL_LOCAL}:{self.PORT}/tts/language"
        data = {
            "id": choice
        }
        requests.request("POST", url, json=data)
        self.current_language = choice
        log_print(f"Changed language to: {choice}")#20260612_kpopmodder
        speaker_names = self.get_speaker_names()
        self.current_speaker = speaker_names[0]
        return gr.update(choices=speaker_names, value=self.current_speaker)

    def on_speaker_change(self, choice):
        self.current_speaker = choice
        log_print(f"Changed speaker to: {choice}")#20260612_kpopmodder
