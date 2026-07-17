
import io
import gradio as gr
from plugin_system.interfaces import TTSPluginInterface
#import pyaudio#20260616_kpopmodder
#import wave#20260616_kpopmodder
import numpy as np
#from pydub import AudioSegment#20260616_kpopmodder
import os
import torch
from torch import no_grad, LongTensor
import argparse

from .vits.commons import * 
from .vits.utils import *
from .vits.mel_processing import spectrogram_torch
from .vits.models import SynthesizerTrn
import gradio as gr
import librosa
import soundfile as sf

from .vits.text import text_to_sequence, _clean_text

from core.gpu_device_manager import gpu_device_manager
from core.logger import log_print, debug_print#20260612_kpopmodder

current_module_directory = os.path.dirname(__file__)
model_dir = os.path.join(current_module_directory, "models")
output_dir = os.path.join(current_module_directory, "output.wav")
config_dir = os.path.join(model_dir, "trilingual.json")
model = os.path.join(model_dir, "trilingual.pth")
language_marks = {
    "Japanese": "",
    "日本語": "[JA]",
    "简体中文": "[ZH]",
    "English": "[EN]",
    "Mix": "",
}
lang = ['日本語', '简体中文', 'English', 'Mix']


class VitsTTS(TTSPluginInterface):
    PLUGIN_METADATA = {
        "id": "VitsTTS",
        "display_name": "VITS TTS",
        "api_version": "1",
        "category": "text_to_speech",
        "entrypoint": "plugins.vitsTTS.vitsTTS:VitsTTS",
        "dependency_group": "Full",
        "capabilities": ("text_to_speech", "vits"),
        "required_python_packages": ("torch", "librosa", "soundfile", "numpy"),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": True,
        "supports_cpu": True,
    }

    def init(self):
        #20260717_kpopmodder: Resolve CUDA only during init so importing the plugin is definition-only.
        self.device = gpu_device_manager.get_device("VitsTTS", default="cuda:0")
        hps = get_hparams_from_file(config_dir)
        net_g = SynthesizerTrn(
            len(hps.symbols),
            hps.data.filter_length // 2 + 1,
            hps.train.segment_size // hps.data.hop_length,
            n_speakers=hps.data.n_speakers,
            **hps.model).to(self.device)
        _ = net_g.eval()

        _ = load_checkpoint(model, net_g, None)
        speaker_ids = hps.speakers
        self.speakers = list(hps.speakers.keys())
        self.tts_fn = self.create_tts_fn(net_g, hps, speaker_ids)

        self.character = self.speakers[0]
        self.language = lang[2]
        self.duration = 1

    def synthesize(self, text):
        log_print(f"self.tts_fn({text}, {self.character}, {self.language}, {self.duration})")#20260612_kpopmodder
        text_output, (sampling_rate, audio) = self.tts_fn(text, self.character, self.language, self.duration)
        sf.write(output_dir, audio, samplerate=sampling_rate, format='WAV')
    
        # Save the audio to a BytesIO stream as a WAV file to return bytes
        with io.BytesIO() as audio_buffer:
            sf.write(audio_buffer, audio, samplerate=sampling_rate, format='WAV')
            audio_buffer.seek(0)  # Go to the start of the buffer
            wav_bytes = audio_buffer.read()  # Read the WAV file bytes
            
        return wav_bytes


    def create_ui(self):
        with gr.Accordion(label="Vits Options", open=False):
            with gr.Row():
                # select character
                self.char_dropdown = gr.Dropdown(choices=self.speakers, value=self.character, label='character')
                self.language_dropdown = gr.Dropdown(choices=lang, value=self.language, label='language')
                self.duration_slider = gr.Slider(minimum=0.1, maximum=5, value=self.duration, step=0.1,
                                            label='速度 Speed')
                
        self.char_dropdown.change(self.update_character, inputs=[self.char_dropdown])
        self.language_dropdown.change(self.update_language, inputs=[self.language_dropdown])
        self.duration_slider.change(self.update_duration, inputs=[self.duration_slider])
                   
        
    def update_character(self, input):
        self.character = input
    
    def update_language(self, input):
        self.language = input
    
    def update_duration(self, input):
        self.duration = input
    

    def get_text(self, text, hps, is_symbol):
        text_norm = text_to_sequence(text, hps.symbols, [] if is_symbol else hps.data.text_cleaners)
        if hps.data.add_blank:
            text_norm = intersperse(text_norm, 0)
        text_norm = LongTensor(text_norm)
        return text_norm

    def create_tts_fn(self, model, hps, speaker_ids):
        def tts_fn(text, speaker, language, speed):
            if language is not None:
                text = language_marks[language] + text + language_marks[language]
            speaker_id = speaker_ids[speaker]
            stn_tst = self.get_text(text, hps, False)
            with no_grad():
                x_tst = stn_tst.unsqueeze(0).to(self.device)
                x_tst_lengths = LongTensor([stn_tst.size(0)]).to(self.device)
                sid = LongTensor([speaker_id]).to(self.device)
                audio = model.infer(x_tst, x_tst_lengths, sid=sid, noise_scale=.667, noise_scale_w=0.8,
                                    length_scale=1.0 / speed)[0][0, 0].data.cpu().float().numpy()
            del stn_tst, x_tst, x_tst_lengths, sid
            return "Success", (hps.data.sampling_rate, audio)

        return tts_fn
