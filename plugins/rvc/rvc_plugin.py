# import io
# import re
# from LAV_utils import download_and_extract_zip
# from .inferrvc import load_torchaudio
# from .inferrvc import RVC
# import edge_tts
# import os
# from plugin_system.interfaces import TTSPluginInterface
# import asyncio
# import gradio as gr
# from pydub import AudioSegment
# import numpy as np
# import soundfile as sf
# from .edge_tts_voices import SUPPORTED_VOICES
# import shutil

# import time

# import uuid#20260611_kpopmodder

# from core.logger import log_print, debug_print#20260612_kpopmodder

# class RVCPlugin(TTSPluginInterface):

#     current_module_directory = os.path.dirname(__file__)
#     EDGE_TTS_OUTPUT_FILENAME = os.path.join(
#         current_module_directory, "edgetts_output.mp3")
#     RVC_OUTPUT_FILENAME = os.path.join(
#         current_module_directory, "rvc_output.wav")
#     rvc_model_dir = os.path.join(current_module_directory, "rvc_model_dir")
#     rvc_index_dir = os.path.join(current_module_directory, "rvc_index_dir")

#     edge_tts_voice = "en-US-AnaNeural"
#     rvc_model_name = 'qiqi.pth'
#     use_rvc = True
#     transpose = 0
#     index_rate = .75
#     protect = 0.5

#     def init(self):
#         # where model.pth files are stored.
#         os.environ['RVC_MODELDIR'] = self.rvc_model_dir
#         # where model.index files are stored.
#         os.environ['RVC_INDEXDIR'] = self.rvc_index_dir
#         # the audio output frequency, default is 44100.
#         os.environ['RVC_OUTPUTFREQ'] = '44100'
#         # If the output audio tensor should block until fully loaded, this can be ignored. But if you want to run in a larger torch pipeline, setting to False will improve performance a little.
#         os.environ['RVC_RETURNBLOCKING'] = 'False'

#         if not os.path.exists(os.path.join(self.current_module_directory, "rvc_model_dir", self.rvc_model_name)):
#             self.download_model_from_url(
#                 "https://huggingface.co/zAnonymousWizard/genshinqiqi/resolve/main/qiqigenshin.zip?download=true")

#         self.model = RVC(self.rvc_model_name)
#         self.update_rvc_model_list()

#     def synthesize(self, text):
#         if text is None:#20260611_kpopmodder
#             return None

#         text = text.strip()#20260611_kpopmodder

#         if text == "":#20260611_kpopmodder
#             return None

#         unique_id = uuid.uuid4().hex#20260611_kpopmodder

#         edge_mp3 = os.path.join(#20260611_kpopmodder
#             self.current_module_directory,
#             f"edgetts_output_{unique_id}.mp3"
#         )

#         edge_wav = os.path.join(#20260611_kpopmodder
#             self.current_module_directory,
#             f"edgetts_output_{unique_id}.wav"
#         )

#         rvc_wav = os.path.join(#20260611_kpopmodder
#             self.current_module_directory,
#             f"rvc_output_{unique_id}.wav"
#         )

#         wav_bytes = None#20260611_kpopmodder

#         try:#20260611_kpopmodder
#             text = self.preprocess_text(text)
#      #       log_print(f'Outputting audio to {self.EDGE_TTS_OUTPUT_FILENAME}')#20260612_kpopmodder
#             log_print(f'Outputting audio to {edge_mp3}')#20260612_kpopmodder
#             try:
#                 communicate = edge_tts.Communicate(text, self.edge_tts_voice)
#      #           asyncio.run(communicate.save(self.EDGE_TTS_OUTPUT_FILENAME))#20260611_kpopmodder
#                 asyncio.run(communicate.save(edge_mp3))#20260611_kpopmodder
            
#                 # Load the MP3 file
#     #            audio = AudioSegment.from_mp3(self.EDGE_TTS_OUTPUT_FILENAME)#20260611_kpopmodder
#                 audio = AudioSegment.from_mp3(edge_mp3)#20260611_kpopmodder

#                 # Convert it to WAV format
#      #           wav_filename = self.EDGE_TTS_OUTPUT_FILENAME.replace('.mp3', '.wav')#20260611_kpopmodder
#                 wav_filename = edge_wav#20260611_kpopmodder

#                 audio.export(wav_filename, format='wav')
#                 audio = AudioSegment.from_wav(wav_filename)
#                 samples = np.array(audio.get_array_of_samples())
#             except Exception as e:
#                 log_print(f"Error converting text {text} to audio: {e}")#20260612_kpopmodder
#                 return None


#             if self.use_rvc:
#                 start_time = time.time()
#                 aud, sr = load_torchaudio(wav_filename)
#                 log_print(f"load_torchaudio: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#                 start_time = time.time()
#                 paudio1 = self.model(aud, f0_up_key=self.transpose,
#                                     output_volume=RVC.MATCH_ORIGINAL, index_rate=self.index_rate, protect=self.protect)
#                 log_print(f"model processing: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#                 start_time = time.time()
#                 paudio1_cpu = paudio1.cpu().numpy()  # Move to CPU and convert to NumPy
#                 #log_print(f"cpu and numpy conversion: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#                 start_time = time.time()
#     #            sf.write(self.RVC_OUTPUT_FILENAME, paudio1_cpu, 44100)#20260611_kpopmodder
#                 sf.write(rvc_wav, paudio1_cpu, 44100)#20260611_kpopmodder

#                 #log_print(f"write audio: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder

#                 start_time = time.time()
#     #            audio = AudioSegment.from_wav(self.RVC_OUTPUT_FILENAME)#20260611_kpopmodder
#                 audio = AudioSegment.from_wav(rvc_wav)#20260611_kpopmodder

#                 buffer = io.BytesIO()
#                 audio.export(buffer, format='wav')  # Export as WAV format
#                 wav_bytes = buffer.getvalue()  # Get the byte value of the audio
#                 #log_print(f"audio segment processing and export: {time.time() - start_time:.5f} seconds")#20260612_kpopmodder
#             return wav_bytes
#         except Exception as e:#20260611_kpopmodder
#             log_print(f"Error converting text {text} to audio: {e}")#20260612_kpopmodder
#             return None

#         finally:#20260611_kpopmodder
#             for f in [edge_mp3, edge_wav, rvc_wav]:
#                 try:
#                     if os.path.exists(f):
#                         os.remove(f)
#                 except Exception:
#                     pass

#     def create_ui(self):
#         with gr.Accordion(label="rvc Options", open=False):
#             with gr.Row():
#                 self.edge_tts_speaker_dropdown = gr.Dropdown(
#                     choices=SUPPORTED_VOICES,
#                     value=self.edge_tts_voice,
#                     label="edge_tts_speaker: "
#                 )

#             with gr.Row():
#                 self.use_rvc_checkbox = gr.Checkbox(
#                     label='Use RVC', value=self.use_rvc)
#                 self.rvc_model_dropdown = gr.Dropdown(label="RVC models:",
#                                                       choices=self.rvc_model_names, value=self.rvc_model_name if len(self.rvc_model_names) > 0 else None, interactive=True)
#                 self.refresh_button = gr.Button("Refresh", variant="primary")

#             with gr.Row():
#                 self.download_model_input = gr.Textbox(label="Model url:")
#                 self.download_button = gr.Button("Download")
#             gr.Markdown(
#                 "You can find models here: https://voice-models.com/top")

#             with gr.Column():
#                 self.transpose_slider = gr.Slider(value=self.transpose,
#                                                   minimum=-24, maximum=24, step=1, label='Transpose')
#                 self.index_rate_slider = gr.Slider(value=self.index_rate,
#                                                    minimum=0, maximum=1, step=0.01, label='Index Rate')
#                 self.protect_slider = gr.Slider(value=self.protect, minimum=0, maximum=0.5,
#                                                 step=0.01, label='Protect')

#                 self.rvc_model_dropdown.input(self.on_rvc_model_change, inputs=[
#                     self.rvc_model_dropdown], outputs=[])
#                 self.refresh_button.click(
#                     self.on_refresh, outputs=[self.rvc_model_dropdown])

#                 self.edge_tts_speaker_dropdown.input(self.on_speaker_change, inputs=[
#                     self.edge_tts_speaker_dropdown], outputs=[])

#                 self.use_rvc_checkbox.change(
#                     self.on_use_rvc_click, self.use_rvc_checkbox, None)
#                 self.transpose_slider.change(
#                     self.on_transpose_change, self.transpose_slider, None)
#                 self.index_rate_slider.change(
#                     self.on_index_rate_change, self.index_rate_slider, None)
#                 self.protect_slider.change(
#                     self.on_protect_change, self.protect_slider, None)

#                 self.download_button.click(
#                     self.download_model_from_url, inputs=self.download_model_input)

#     def on_transpose_change(self, value):
#         self.transpose = value

#     def on_index_rate_change(self, value):
#         self.index_rate = value

#     def on_protect_change(self, value):
#         self.protect = value

#     def on_use_rvc_click(self, use):
#         self.use_rvc = use

#     def on_speaker_change(self, choice):
#         self.edge_tts_voice = choice

#     def on_rvc_model_change(self, choice):
#         self.rvc_model_name = choice
#         self.model = RVC(self.rvc_model_name)

#     def on_refresh(self):
#         self.update_rvc_model_list()
#         return gr.update(choices=self.rvc_model_names)

#     def update_rvc_model_list(self):
#         self.rvc_model_names = []
#         for name in os.listdir(self.rvc_model_dir):
#             if name.endswith(".pth"):
#                 self.rvc_model_names.append(name)

#     def download_model_from_url(self, url):
#         folder_path = download_and_extract_zip(
#             url, extract_to=self.current_module_directory)

#         # Find the .pth file and get its base name
#         for file in os.listdir(folder_path):
#             if file.endswith('.pth'):
#                 base_name = os.path.splitext(file)[0]
#                 pth_file_path = os.path.join(folder_path, file)
#                 break

#         if pth_file_path and base_name:
#             # Look for the corresponding .index file
#             for file in os.listdir(folder_path):
#                 if file.endswith('.index'):
#                     original_index_file_path = os.path.join(folder_path, file)
#                     new_index_file_path = os.path.join(
#                         folder_path, base_name + '.index')
#                     os.rename(original_index_file_path, new_index_file_path)

#                     # Move the .pth file
#                     shutil.move(pth_file_path, os.path.join(
#                         self.rvc_model_dir, os.path.basename(pth_file_path)))

#                     # Move the .index file
#                     shutil.move(new_index_file_path, os.path.join(
#                         self.rvc_index_dir, os.path.basename(new_index_file_path)))

#                     # Remove the folder once done
#                     try:
#                         # Use this if the folder is expected to be empty
#                         os.rmdir(folder_path)
#                     except OSError:
#                         # Use this if the folder might contain other files
#                         shutil.rmtree(folder_path)
#                     break
#             else:
#                 log_print(f"No .index file found for {base_name}")#20260612_kpopmodder
#         else:
#             log_print("No .pth file found in the folder.")#20260612_kpopmodder
#     def preprocess_text(self, text):
#         log_print(f"replacing decimal point with the word point.")#20260612_kpopmodder
#         log_print(f"original:) {text}")#20260612_kpopmodder

#         pattern = r'\b\d*\.\d+\b'

#         def replace_match(match):
#             decimal_number = match.group(0)
#             return decimal_number.replace('.', ' point ')

#         # Replace all occurrences of decimal patterns in the text
#         replaced_text = re.sub(pattern, replace_match, text)
#         log_print(f"replaced_text: {replaced_text}")#20260612_kpopmodder
        
#         return replaced_text