#import io#20260617_kpopmodder
#import os#20260616_kpopmodder
from queue import Queue, Empty
#import shutil#20260616_kpopmodder
import ctypes#20260705_kpopmodder: Poll Win32 key state when game focus blocks keyboard hotkeys.
import threading
#import zipfile#20260616_kpopmodder
import time
#import re#20260616_kpopmodder

#import requests#20260616_kpopmodder
#from tqdm import tqdm#20260616_kpopmodder
from ui_core.live_textbox import LiveTextbox
from plugin_system.interfaces import TTSPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase
#from pydub import AudioSegment#20260617_kpopmodder
#import simpleaudio as sa#20260617_kpopmodder
#from pydub.utils import audioop#20260617_kpopmodder

from core.event_manager import event_manager, EventType
#from core.global_state import global_state, GlobalKeys#20260617_kpopmodder
from core.logger import log_print
from safety_filter import clean_text
#from audio_device_manager import audio_device_manager#20260616_kpopmodder

try:#20260705_kpopmodder: Optional global hotkey support for emergency TTS stop.
    import keyboard
except Exception:
    keyboard = None

from tts_core.text_processor import TTSTextProcessor#20260617_kpopmodder
from tts_core.ffmpeg_manager import ensure_ffmpeg_exists#20260617_kpopmodder

#import os#20260617_kpopmodder
#import tempfile#20260617_kpopmodder
#import winsound#20260617_kpopmodder
#import wave#20260617_kpopmodder

from tts_core.mouth_animator import TTSMouthAnimator#20260617_kpopmodder
from tts_core.winsound_player import WinSoundAudioPlayer#20260617_kpopmodder

from tts_core.tts_queue_worker import TTSQueueWorker#20260617_kpopmodder

from tts_core.tts_interrupt_controller import TTSInterruptController#20260617_kpopmodder

class TTS(PluginSelectionBase):#20260615_kpopmodder
    #output_event_listeners = []#20260616_kpopmodder

    #playback_queue_live_textbox = LiveTextbox()#20260616_kpopmodder

    subtitle_file_path = "subtitle.txt"
    #VOICE_OUTPUT_FILENAME = "synthesized_voice.wav"#20260616_kpopmodder

    def __init__(self) -> None:
        super().__init__(TTSPluginInterface)

        self.log_live_textbox = LiveTextbox()
        self.process_queue_live_textbox = LiveTextbox()
        self.input_queue = Queue()
        #self.audio_data_queue = Queue()#20260616_kpopmodder

        self.audio_process_thread = None
        #self.audio_playback_thread = None#20260616_kpopmodder
        self.output_event_listeners = []#20260616_kpopmodder

        self.interrupt_event = threading.Event()
        self.queue_lock = threading.Lock()
        self.queue_generation_lock = threading.Lock()#20260621_kpopmodder
        self.queue_generation = 0#20260621_kpopmodder: 인터럽트 전/후 TTS 큐 섞임 방지용 세대 번호
        self.response_generation_lock = threading.Lock()#20260623_kpopmodder
        self.latest_response_generation = None#20260623_kpopmodder: Drop pending TTS from older LLM responses.
        self.audio_lock = threading.RLock()
        self.synth_lock = threading.Lock()

        self.output_lock = threading.Lock()#20260617_kpopmodder
        self.last_output_send_time = 0.0#20260617_kpopmodder
        #self.output_min_interval = 0.05  # 20fps#20260617_kpopmodder

        #self.disable_mouth_output = True  #20260617_kpopmodder - winsound stability mode

        self.disable_mouth_output = False  #20260617_kpopmodder - mouth output restored safely
        #self.mouth_thread = None#20260617_kpopmodder
        #self.mouth_stop_event = threading.Event()#20260617_kpopmodder
        self.mouth_animator = TTSMouthAnimator(#20260617_kpopmodder
            output_callback=self.send_output,
            interrupt_event=self.interrupt_event,
            disabled_callback=lambda: self.disable_mouth_output,
            target_fps=12,
        )

        self.audio_player = WinSoundAudioPlayer(#20260617_kpopmodder
            interrupt_event=self.interrupt_event,
            mouth_animator=self.mouth_animator,
        )
        self.output_min_interval = 0.08  #20260617_kpopmodder - safer 12.5fps

        #self.audio_device_manager = audio_device_manager#20260616_kpopmodder
        
        self.text_processor = TTSTextProcessor()#20260616_kpopmodder

        self.queue_worker = TTSQueueWorker(self)#20260617_kpopmodder

        self.interrupt_controller = TTSInterruptController(self)#20260617_kpopmodder

        #self.check_ffmpeg()#20260616_kpopmodder
        #ensure_ffmpeg_exists()#20260616_kpopmodder
        self.configure_audio_runtime()#20260716_kpopmodder

        self._shutdown = False
        self._interrupt_subscription = event_manager.subscribe(
            EventType.INTERRUPT,
            self.handle_interrupt,
        )
        self.stop_hotkey = "ctrl+shift+alt+q"#20260705_kpopmodder: Emergency TTS stop hotkey during games.
        self.stop_hotkey_handle = None#20260705_kpopmodder
        self.stop_hotkey_poll_stop_event = threading.Event()#20260705_kpopmodder
        self.stop_hotkey_poll_thread = None#20260705_kpopmodder
        self.last_stop_hotkey_time = 0.0#20260705_kpopmodder
        self.configure_stop_hotkey_controls()#20260716_kpopmodder

    def create_ui(self):
        with gr.Tab("TTS"):
            super().create_plugin_selection_ui()

            self.main_interface = gr.Interface(
                fn=self.wrapper_synthesize,
                inputs=[gr.Textbox(label="Original Text")],
                outputs=[gr.Audio(label="Synthesized Voice")],
                #allow_flagging="never",#20260615_kpopmodder
                flagging_mode="never",#20260615_kpopmodder
                examples=[
                    "すぅ…はぁ——おはようさん、朝の空気は清々しくて気持ちええなぁ、深呼吸して頭もすっきりや。",
                    "金魚飼ったことある？大人しゅうて、めっちゃ可愛ええんや。",
                    "全身ポカポカで気持ちええわぁ～、浮いとるみたい。",
                    "Ah... *yawns* Good morning. The morning air is the freshest. Come on, take a few extra breaths — it'll make you smarter~",
                    "Have you ever kept goldfish as pets? They're very cute.",
                    "Ah, this is great! I feel so relaxed all over, I could almost float away.",
                    "hello",
                ],
            )

            gr.Markdown("Note: Some prividers may only support certain languages.")

            with gr.Accordion("Console", open=False):
                self.log_live_textbox.create_ui()
                self.process_queue_live_textbox.create_ui(
                    lines=3,
                    max_lines=3,
                    label="Input waiting to be processed: ",
                )
                # self.playback_queue_live_textbox.create_ui(#20260616_kpopmodder
                #     lines=3,
                #     max_lines=3,
                #     label="Generated audio waiting to be played: ",
                # )

            super().create_plugin_ui()

    def wrapper_synthesize(self, text):
        text = clean_text(text)

        result = self.current_plugin.synthesize(text)
        self.update_subtitle_file(text)
        self.play_sound_from_bytes(result)

        return result

    def receive_input(self, text):
        text, response_generation = self.unpack_input_payload(text)#20260623_kpopmodder
        items = self.prepare_input_items(text)

        if not items:
            return

        self.enqueue_input_items(
            items,
            response_generation=response_generation,
        )#20260623_kpopmodder
        self.process_input_queue(self.current_plugin.synthesize)

    def prepare_input_items(self, text):
        if isinstance(text, list):
            return self.prepare_list_input_items(text)

        if isinstance(text, str):
            return self.prepare_string_input_items(text)

        log_print(f"TTS: unsupported input type: {type(text)}")
        return []

    def prepare_list_input_items(self, text_list):
        if not all(isinstance(item, str) for item in text_list):
            log_print("TTS: The list must contain only strings.")
            return []

        merged_items = []
        buffer = ""

        for item in text_list:
            #item = self.normalize_text_item(item)#20260616_kpopmodder
            item = self.text_processor.normalize_text_item(item)#20260616_kpopmodder

            if item == "":
                continue

            if buffer:
                buffer += " " + item
            else:
                buffer = item

            if len(buffer) >= 15:
                merged_items.append(buffer)
                buffer = ""

        if buffer:
            merged_items.append(buffer)

        return merged_items

    def prepare_string_input_items(self, text):
        #text = self.normalize_text_item(text)#20260616_kpopmodder
        text = self.text_processor.normalize_text_item(text)#20260616_kpopmodder

        if text == "":
            log_print("TTS: ignoring empty input")
            return []

        items = self.text_processor.split_tts_sentences(text)#20260616_kpopmodder
        log_print(f"[TTS split] {items}")#20260616_kpopmodder

        #return self.split_tts_sentences(text)#20260616_kpopmodder
        #return self.text_processor.split_tts_sentences(text)#20260616_kpopmodder

        return items#20260616_kpopmodder

    # def normalize_text_item(self, text):
    #     if text is None:
    #         return ""

    #     text = str(text).strip()
    #     text = clean_text(text)
    #     text = text.strip()

    #     return text

    # def is_tts_skippable(self, text):
    #     text = str(text).strip()#20260616_kpopmodder

    #     if not text:
    #         return True

    #     # 한글/영문/숫자가 하나도 없으면 스킵
    #     if not re.search(r"[가-힣a-zA-Z0-9]", text):
    #         return True

    #     return False

    # def enqueue_input_items(self, items):#20260621_kpopmodder
    #     with self.queue_lock:
    #         # for item in items:#20260616_kpopmodder
    #         #     if item:
    #         #         self.input_queue.put(item)
    #         for item in items:#20260616_kpopmodder
    #             if not item:
    #                 continue

    #             #if self.is_tts_skippable(item):#20260616_kpopmodder
    #             if self.text_processor.is_tts_skippable(item):#20260616_kpopmodder
    #                 log_print(f"[TTS QUEUE] skipped non-speech text: {item}")
    #                 continue

    #             self.input_queue.put(item)

    #     self.process_queue_live_textbox.set(
    #         LAV_utils.queue_to_list(self.input_queue)
    #     )

    def enqueue_input_items(self, items, response_generation=None):#20260621_kpopmodder
        with self.queue_lock:
            generation = self.get_queue_generation()#20260621_kpopmodder
            if self.is_stale_response_generation(response_generation):#20260623_kpopmodder
                log_print(
                    "[TTS QUEUE] dropped stale response items before enqueue: "
                    f"response_generation={response_generation}, items={items}"
                )
                return

            if self.update_latest_response_generation(response_generation):#20260623_kpopmodder
                dropped_count = self.drop_queued_older_response_items(
                    response_generation
                )
                if dropped_count:
                    log_print(
                        "[TTS QUEUE] dropped older queued response sentences: "
                        f"response_generation={response_generation}, count={dropped_count}"
                    )
            for item in items:#20260616_kpopmodder
                if not item:
                    continue
                if self.text_processor.is_tts_skippable(item):#20260616_kpopmodder
                    log_print(f"[TTS QUEUE] skipped non-speech text: {item}")
                    continue
                #20260621_kpopmodder: 인터럽트 이후 이전 세대 문장이 재생되지 않도록 세대 번호와 함께 저장한다.
                self.input_queue.put(
                    self.make_queue_item(
                        generation,
                        item,
                        response_generation=response_generation,
                    )
                )
            self.process_queue_live_textbox.set(self.get_queue_display_items())

    def process_input_queue(self, function):
        #self.start_tts_worker_if_needed(function)#20260617_kpopmodder
        self.queue_worker.start_if_needed(function)#20260617_kpopmodder

    # def start_tts_worker_if_needed(self, synthesize_function):#20260617_kpopmodder
    #     if self.audio_process_thread is not None and self.audio_process_thread.is_alive():
    #         return

    #     self.audio_process_thread = threading.Thread(
    #         target=self.tts_worker_loop,
    #         args=(synthesize_function,),
    #     )
    #     self.audio_process_thread.daemon = True
    #     self.audio_process_thread.start()

    # def tts_worker_loop(self, synthesize_function):#20260617_kpopmodder
    #     self.start_tts_speaking_state()

    #     try:
    #         while True:
    #             input_text = self.get_next_input_text()

    #             if input_text is None:
    #                 break

    #             if self.interrupt_event.is_set():
    #                 log_print("[TTS QUEUE] interrupt detected before synthesize")
    #                 break

    #             log_print(f"[TTS QUEUE] start sentence: {input_text}")

    #             audio_result = self.synthesize_with_retry(
    #                 synthesize_function,
    #                 input_text,
    #                 max_retries=3,
    #             )

    #             if audio_result is None:
    #                 log_print(
    #                     f"[TTS QUEUE] synthesize finally failed after retries: {input_text}"
    #                 )
    #                 continue

    #             if self.interrupt_event.is_set():
    #                 log_print("[TTS QUEUE] interrupt detected after synthesize")
    #                 break

    #             self.update_subtitle_file(input_text)

    #             playback_ok = self.playback_with_retry(
    #                 audio_result,
    #                 input_text,
    #                 max_retries=2,
    #             )

    #             if self.interrupt_event.is_set():#20260616_kpopmodder
    #                 log_print("[TTS QUEUE] interrupt detected during playback")
    #                 break

    #             if not playback_ok:
    #                 log_print(
    #                     f"[TTS QUEUE] playback finally failed. force skip: {input_text}"
    #                 )
    #                 continue

    #             log_print(f"[TTS QUEUE] finished sentence: {input_text}")

    #             self.process_queue_live_textbox.set(
    #                 LAV_utils.queue_to_list(self.input_queue)
    #             )

    #     except Exception as e:
    #         log_print(f"[TTS process_input_queue error] {e}")

    #     finally:
    #         self.finish_tts_speaking_state()

    # def start_tts_speaking_state(self):#20260617_kpopmodder
    #     global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)
    #     debug_print("[TTS QUEUE] IS_AI_SPEAKING=True")

    # def finish_tts_speaking_state(self):#20260617_kpopmodder
    #     self.interrupt_event.clear()

    #     global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)
    #     global_state.set_value(
    #         GlobalKeys.LAST_AI_SPEAK_END_TIME,
    #         time.time(),
    #     )

    #     self.send_output(0)
    #     debug_print("[TTS QUEUE] IS_AI_SPEAKING=False")

    # def get_next_input_text(self):#20260617_kpopmodder
    #     with self.queue_lock:
    #         if self.input_queue.empty():
    #             return None

    #         input_text = self.input_queue.get()

    #     input_text = str(input_text).strip()

    #     if not input_text:
    #         return None#20260617_kpopmodder

    #     return input_text

    # def synthesize_with_retry(self, synthesize_function, input_text, max_retries=3):#20260617_kpopmodder
    #     audio_result = None

    #     for retry_count in range(max_retries):
    #         try:
    #             with self.synth_lock:
    #                 audio_result = synthesize_function(input_text)

    #             if audio_result is not None:
    #                 return audio_result

    #             log_print(
    #                 f"[TTS QUEUE] synthesize returned None. "
    #                 f"retry={retry_count + 1}/{max_retries} text={input_text}"
    #             )

    #         except Exception as e:
    #             log_print(
    #                 f"[TTS QUEUE] synthesize failed. "
    #                 f"retry={retry_count + 1}/{max_retries} "
    #                 f"text={input_text}, error={e}"
    #             )

    #         time.sleep(1.0)

    #     return None

    # def playback_with_retry(self, audio_result, input_text, max_retries=2):#20260616_kpopmodder
    #     for retry_count in range(max_retries):
    #         try:
    #             with self.audio_lock:
    #                 self.play_sound_from_bytes(audio_result)

    #             return True

    #         except Exception as e:
    #             log_print(
    #                 f"[TTS QUEUE] playback failed. "
    #                 f"retry={retry_count + 1}/{max_retries} "
    #                 f"text={input_text}, error={e}"
    #             )

    #         time.sleep(0.5)

    #     return False

    # def playback_with_retry(self, audio_result, input_text, max_retries=2):#20260617_kpopmodder
    #     for retry_count in range(max_retries):
    #         if self.interrupt_event.is_set():
    #             return False

    #         try:
    #             with self.audio_lock:
    #                 playback_ok = self.play_sound_from_bytes(audio_result)

    #             if playback_ok:
    #                 return True

    #             if self.interrupt_event.is_set():
    #                 return False

    #             log_print(
    #                 f"[TTS QUEUE] playback returned False. "
    #                 f"retry={retry_count + 1}/{max_retries} "
    #                 f"text={input_text}"
    #             )

    #         except Exception as e:
    #             log_print(
    #                 f"[TTS QUEUE] playback failed. "
    #                 f"retry={retry_count + 1}/{max_retries} "
    #                 f"text={input_text}, error={e}"
    #             )

    #         time.sleep(0.5)

    #     return False

    def update_subtitle_file(self, text):
        with open(self.subtitle_file_path, "w", encoding="utf-8") as file:
            file.write(text)

    # def process_audio_queue(self, function):#20260616_kpopmodder
    #     return

    # def find_max_rms(self, audio_segment, chunk_size=1024):#20260617_kpopmodder
    #     max_rms = 0

    #     for i in range(0, len(audio_segment.raw_data), chunk_size):
    #         chunk_data = audio_segment.raw_data[i:i + chunk_size]
    #         rms = audioop.rms(chunk_data, audio_segment.sample_width)

    #         if rms > max_rms:
    #             max_rms = rms

    #     return max_rms

    # def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260616_kpopmodder
    #     if audio_data is None:
    #         return

    #     play_obj = None

    #     try:
    #         audio = self.load_audio_segment(audio_data)
    #         max_rms = self.find_max_rms(audio, chunk_size)

    #         if max_rms <= 0:
    #             return

    #         log_print("[TTS playback] using simpleaudio for stability")

    #         play_obj = self.start_simpleaudio_playback(audio)

    #         self.monitor_playback(
    #             audio=audio,
    #             play_obj=play_obj,
    #             max_rms=max_rms,
    #             chunk_size=chunk_size,
    #         )

    #         self.wait_playback_done(play_obj)

    #     except Exception as e:
    #         import traceback
    #         log_print(f"[TTS playback error] {e}")
    #         log_print(traceback.format_exc())

    #     finally:
    #         if self.interrupt_event.is_set():
    #             self.stop_play_obj_safely(play_obj)

    #         self.send_output(0)

    # def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260617_kpopmodder
    #     if audio_data is None:
    #         return False

    #     play_obj = None

    #     try:
    #         audio = self.load_audio_segment(audio_data)
    #         max_rms = self.find_max_rms(audio, chunk_size)

    #         if max_rms <= 0:
    #             log_print("[TTS playback] silent audio detected. skip playback.")
    #             return False

    #         log_print("[TTS playback] using simpleaudio for stability")

    #         play_obj = self.start_simpleaudio_playback(audio)

    #         playback_ok = self.monitor_playback(
    #             audio=audio,
    #             play_obj=play_obj,
    #             max_rms=max_rms,
    #             chunk_size=chunk_size,
    #         )

    #         if not playback_ok:
    #             return False

    #         return self.wait_playback_done(play_obj)

    #     except Exception as e:
    #         import traceback
    #         log_print(f"[TTS playback error] {e}")
    #         log_print(traceback.format_exc())
    #         return False

    #     finally:
    #         if self.interrupt_event.is_set():
    #             self.stop_play_obj_safely(play_obj)

    #         self.send_output(0)

    # def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260617_kpopmodder
    #     if audio_data is None:
    #         return False

    #     temp_wav_path = None

    #     try:
    #         if self.interrupt_event.is_set():
    #             return False

    #         temp_wav_path = self.write_temp_wav_file(audio_data)

    #         log_print(
    #             f"[TTS playback] using winsound for stability. "
    #             f"bytes={len(audio_data)}, file={temp_wav_path}"
    #         )

    #         self.start_mouth_animation(audio_data)#20260617_kpopmodder

    #         winsound.PlaySound(
    #             temp_wav_path,
    #             winsound.SND_FILENAME,
    #         )

    #         if self.interrupt_event.is_set():
    #             return False

    #         log_print("[TTS playback] winsound play done")
    #         return True

    #     except Exception as e:
    #         import traceback
    #         log_print(f"[TTS playback error] {e}")
    #         log_print(traceback.format_exc())
    #         return False

    #     finally:
    #         self.stop_mouth_animation()#20260617_kpopmodder

    #         if self.interrupt_event.is_set():
    #             self.stop_winsound_safely()

    #         self.delete_temp_wav_file_safely(temp_wav_path)
    #         self.send_output(0)

    def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260617_kpopmodder
        playback_ok = self.audio_player.play_from_bytes(audio_data)
        self.send_output(0)
        return playback_ok

    # def write_temp_wav_file(self, audio_data):#20260617_kpopmodder
    #     fd, temp_wav_path = tempfile.mkstemp(
    #         prefix="lav_tts_",
    #         suffix=".wav",
    #     )

    #     with os.fdopen(fd, "wb") as file:
    #         file.write(audio_data)

    #     return temp_wav_path


    # def delete_temp_wav_file_safely(self, temp_wav_path):#20260617_kpopmodder
    #     if not temp_wav_path:
    #         return

    #     try:
    #         if os.path.exists(temp_wav_path):
    #             os.remove(temp_wav_path)

    #     except Exception as e:
    #         log_print(f"[TTS playback] temp wav delete error: {e}")


    # def stop_winsound_safely(self):#20260617_kpopmodder
    #     try:
    #         winsound.PlaySound(None, 0)

    #     except Exception as e:
    #         log_print(f"[TTS playback] winsound stop error: {e}")

    # def stop_winsound_safely(self):#20260617_kpopmodder
    #     self.audio_player.stop()

    # def start_mouth_animation(self, audio_data):#20260617_kpopmodder
    #     if getattr(self, "disable_mouth_output", False):
    #         return

    #     self.stop_mouth_animation()
    #     self.mouth_stop_event.clear()

    #     self.mouth_thread = threading.Thread(
    #         target=self.mouth_animation_loop,
    #         args=(audio_data,),
    #     )
    #     self.mouth_thread.daemon = True
    #     self.mouth_thread.start()

    # def start_mouth_animation(self, audio_data):#20260617_kpopmodder
    #     self.mouth_animator.start(audio_data)

    # def stop_mouth_animation(self):#20260617_kpopmodder
    #     try:
    #         self.mouth_stop_event.set()

    #         if (
    #             self.mouth_thread
    #             and self.mouth_thread.is_alive()
    #             and threading.current_thread() != self.mouth_thread
    #         ):
    #             self.mouth_thread.join(timeout=0.3)

    #     except Exception as e:
    #         log_print(f"[TTS mouth] stop error: {e}")

    #     finally:
    #         self.send_output(0)

    # def stop_mouth_animation(self):#20260617_kpopmodder
    #     self.mouth_animator.stop()

    # def mouth_animation_loop(self, audio_data):#20260617_kpopmodder
    #     try:
    #         volumes = self.extract_mouth_volumes(
    #             audio_data=audio_data,
    #             target_fps=12,
    #         )

    #         if not volumes:
    #             return

    #         sleep_time = 1.0 / 12.0

    #         for volume in volumes:
    #             if self.mouth_stop_event.is_set():
    #                 break

    #             if self.interrupt_event.is_set():
    #                 break

    #             self.send_output(volume)
    #             time.sleep(sleep_time)

    #     except Exception as e:
    #         log_print(f"[TTS mouth] animation error: {e}")

    #     finally:
    #         self.send_output(0)


    # def extract_mouth_volumes(self, audio_data, target_fps=12):#20260617_kpopmodder
    #     try:
    #         with wave.open(io.BytesIO(audio_data), "rb") as wav_file:
    #             channels = wav_file.getnchannels()
    #             sample_width = wav_file.getsampwidth()
    #             frame_rate = wav_file.getframerate()
    #             frame_count = wav_file.getnframes()

    #             if frame_rate <= 0 or frame_count <= 0:
    #                 return []

    #             frames_per_chunk = max(1, int(frame_rate / target_fps))
    #             bytes_per_frame = channels * sample_width
    #             bytes_per_chunk = frames_per_chunk * bytes_per_frame

    #             raw_data = wav_file.readframes(frame_count)

    #         rms_values = []

    #         for i in range(0, len(raw_data), bytes_per_chunk):
    #             chunk_data = raw_data[i:i + bytes_per_chunk]

    #             if not chunk_data:
    #                 continue

    #             rms = audioop.rms(chunk_data, sample_width)
    #             rms_values.append(rms)

    #         if not rms_values:
    #             return []

    #         max_rms = max(rms_values)

    #         if max_rms <= 0:
    #             return []

    #         volumes = []

    #         for rms in rms_values:
    #             volume = rms / max_rms
    #             volume = min(1.0, max(0.0, volume))

    #             # 너무 작으면 입이 덜덜거리는 정도라 낮춤
    #             if volume < 0.05:
    #                 volume = 0.0

    #             volumes.append(volume)

    #         return volumes

    #     except Exception as e:
    #         log_print(f"[TTS mouth] volume extract error: {e}")
    #         return []

    # def load_audio_segment(self, audio_data):#20260617_kpopmodder
    #     return AudioSegment.from_file(
    #         io.BytesIO(audio_data),
    #         format="wav",
    #     )

    # def start_simpleaudio_playback(self, audio):
    #     return sa.play_buffer(
    #         audio.raw_data,
    #         num_channels=audio.channels,
    #         bytes_per_sample=audio.sample_width,
    #         sample_rate=audio.frame_rate,
    #     )

    # def monitor_playback(self, audio, play_obj, max_rms, chunk_size):#20260616_kpopmodder
    #     sleep_time = self.calculate_chunk_sleep_time(audio, chunk_size)

    #     for i in range(0, len(audio.raw_data), chunk_size):
    #         if self.interrupt_event.is_set():
    #             self.stop_play_obj_safely(play_obj)
    #             self.send_output(0)
    #             return

    #         chunk_data = audio.raw_data[i:i + chunk_size]
    #         self.update_volume_meter(chunk_data, audio, max_rms)

    #         time.sleep(sleep_time)

    # def monitor_playback(self, audio, play_obj, max_rms, chunk_size):#20260617_kpopmodder
    #     sleep_time = self.calculate_chunk_sleep_time(audio, chunk_size)

    #     for i in range(0, len(audio.raw_data), chunk_size):
    #         if self.interrupt_event.is_set():
    #             self.stop_play_obj_safely(play_obj)
    #             self.send_output(0)
    #             return False

    #         chunk_data = audio.raw_data[i:i + chunk_size]
    #         self.update_volume_meter(chunk_data, audio, max_rms)

    #         time.sleep(sleep_time)

    #     return True

    # def monitor_playback(self, audio, play_obj, max_rms, chunk_size):#20260617_kpopmodder
    #     sleep_time = self.calculate_chunk_sleep_time(audio, chunk_size)

    #     for i in range(0, len(audio.raw_data), chunk_size):
    #         if self.interrupt_event.is_set():
    #             self.stop_play_obj_safely(play_obj)
    #             self.send_output(0)
    #             return False

    #         chunk_data = audio.raw_data[i:i + chunk_size]
    #         self.update_volume_meter(chunk_data, audio, max_rms)

    #         time.sleep(sleep_time)

    #     return True

    # def calculate_chunk_sleep_time(self, audio, chunk_size):#20260617_kpopmodder
    #     denominator = audio.frame_rate * audio.channels * audio.sample_width

    #     if denominator <= 0:
    #         return 0.01

    #     return chunk_size / denominator

    # def calculate_audio_duration(self, audio):#20260617_kpopmodder
    #     denominator = audio.frame_rate * audio.channels * audio.sample_width

    #     if denominator <= 0:
    #         return 0.0

    #     return len(audio.raw_data) / denominator

    # def update_volume_meter(self, chunk_data, audio, max_rms):
    #     try:
    #         rms = audioop.rms(chunk_data, audio.sample_width)
    #         #normalized_volume = rms / max_rms#20260617_kpopmodder
    #         normalized_volume = min(1.0, max(0.0, rms / max_rms))#20260617_kpopmodder
    #         self.send_output(normalized_volume)

    #     except Exception as e:
    #         log_print(f"[TTS playback] volume meter error: {e}")

    # def wait_playback_done(self, play_obj):#20260616_kpopmodder
    #     try:
    #         if play_obj is not None:
    #             play_obj.wait_done()

    #     except Exception as e:
    #         log_print(f"[TTS playback] simpleaudio wait_done error: {e}")

    # def wait_playback_done(self, play_obj):#20260617_kpopmodder
    #     try:
    #         if play_obj is not None:
    #             play_obj.wait_done()

    #         return not self.interrupt_event.is_set()

    #     except Exception as e:
    #         log_print(f"[TTS playback] simpleaudio wait_done error: {e}")
    #         return False

    # def wait_playback_done(self, play_obj, timeout_seconds=10.0):#20260617_kpopmodder
    #     if play_obj is None:
    #         return False

    #     start_time = time.time()

    #     try:
    #         while play_obj.is_playing():
    #             if self.interrupt_event.is_set():
    #                 log_print("[TTS playback] interrupted during wait")
    #                 self.stop_play_obj_safely(play_obj)
    #                 return False

    #             elapsed = time.time() - start_time

    #             if elapsed > timeout_seconds:
    #                 log_print(
    #                     f"[TTS playback] wait timeout. "
    #                     f"force stop. timeout={timeout_seconds:.2f}s"
    #                 )
    #                 self.stop_play_obj_safely(play_obj)
    #                 return False

    #             time.sleep(0.05)

    #         log_print("[TTS playback] simpleaudio play done")
    #         return True

    #     except Exception as e:
    #         log_print(f"[TTS playback] simpleaudio wait loop error: {e}")
    #         self.stop_play_obj_safely(play_obj)
    #         return False

    # def stop_play_obj_safely(self, play_obj):#20260617_kpopmodder
    #     try:
    #         if play_obj is not None:
    #             play_obj.stop()

    #     except Exception as e:
    #         log_print(f"[TTS playback] simpleaudio stop error: {e}")

    # def check_ffmpeg(self):#20260616_kpopmodder
    #     if os.path.exists("ffmpeg.exe"):
    #         return

    #     file_name = "ffmpeg-release-essentials.zip"
    #     url = "https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-6.1.1-essentials_build.zip"

    #     log_print(f"Downloading {file_name} from {url}...")

    #     response = requests.get(url, stream=True)

    #     if response.status_code != 200:
    #         log_print(f"ffmpeg download failed. status_code={response.status_code}")
    #         return

    #     total_size_in_bytes = int(response.headers.get("content-length", 0))
    #     block_size = 1024

    #     progress_bar = tqdm(
    #         total=total_size_in_bytes,
    #         unit="iB",
    #         unit_scale=True,
    #     )

    #     with open(file_name, "wb") as file:
    #         for data in response.iter_content(block_size):
    #             progress_bar.update(len(data))
    #             file.write(data)

    #     progress_bar.close()

    #     if total_size_in_bytes != 0 and progress_bar.n != total_size_in_bytes:
    #         log_print("ERROR, something went wrong during download")
    #         return

    #     log_print(f"{file_name} downloaded successfully.")
    #     log_print(f"Extracting {file_name}...")

    #     with zipfile.ZipFile(file_name, "r") as zip_ref:
    #         zip_ref.extractall()

    #     log_print(f"{file_name} extracted successfully.")

    #     current_module_directory = os.path.dirname(__file__)

    #     ffmpeg_exe_path = os.path.join(
    #         current_module_directory,
    #         "ffmpeg-6.1.1-essentials_build",
    #         "bin",
    #         "ffmpeg.exe",
    #     )

    #     ffprobe_exe_path = os.path.join(
    #         current_module_directory,
    #         "ffmpeg-6.1.1-essentials_build",
    #         "bin",
    #         "ffprobe.exe",
    #     )

    #     shutil.move(ffmpeg_exe_path, current_module_directory)
    #     shutil.move(ffprobe_exe_path, current_module_directory)

    #     shutil.rmtree("ffmpeg-6.1.1-essentials_build")
    #     os.remove(file_name)

    # def split_tts_sentences(self, text, max_len=80):
    #     text = text.replace("\n", " ")
    #     text = re.sub(r"\s+", " ", text).strip()

    #     if not text:
    #         return []

    #     raw_sentences = re.split(r"(?<=[.!?。！？…])\s+", text)
    #     result = []

    #     for sentence in raw_sentences:
    #         sentence = sentence.strip()

    #         if not sentence:
    #             continue

    #         result.extend(
    #             self.split_long_sentence(sentence, max_len=max_len)
    #         )

    #     log_print(f"[TTS split] {result}")

    #     return result

    # def split_long_sentence(self, sentence, max_len=80):
    #     result = []

    #     while len(sentence) > max_len:
    #         cut = sentence[:max_len]

    #         split_pos = max(
    #             cut.rfind(","),
    #             cut.rfind(" "),
    #             cut.rfind("，"),
    #         )

    #         if split_pos <= 0:
    #             split_pos = max_len

    #         result.append(sentence[:split_pos].strip())
    #         sentence = sentence[split_pos:].strip()

    #     if sentence:
    #         result.append(sentence)

    #     return result

    # def handle_interrupt(self):
    #     self.interrupt_event.set()
    #     log_print("Interrupting pipeline")

    #     self.stop_winsound_safely()#20260617_kpopmodder
    #     self.stop_mouth_animation()#20260617_kpopmodder
    #     self.clear_queues()
    #     self.close_mouth_safely()
    #     self.join_audio_thread_safely()

    #     if not (#20260616_kpopmodder
    #         self.audio_process_thread
    #         and self.audio_process_thread.is_alive()
    #     ):
    #         self.interrupt_event.clear()

    # def handle_interrupt(self):#20260617_kpopmodder
    #     self.interrupt_event.set()
    #     log_print("Interrupting pipeline")

    #     self.audio_player.stop()
    #     self.mouth_animator.stop()
    #     self.clear_queues()
    #     self.close_mouth_safely()
    #     self.join_audio_thread_safely()

    #     if not (
    #         self.audio_process_thread
    #         and self.audio_process_thread.is_alive()
    #     ):
    #         self.interrupt_event.clear()

    def handle_interrupt(self):#20260617_kpopmodder
        self.interrupt_controller.handle_interrupt()

    def cancel_pending(self, reason=""):#20260711_kpopmodder
        #20260711_kpopmodder: Public TTS-only boundary cancellation used by
        # game integrations without interrupting LLM, Translate, or ScreenVision.
        return self.interrupt_controller.cancel_pending(reason=reason)

    def is_null_tts_provider(self):#20260716_kpopmodder
        current_plugin = getattr(self, "current_plugin", None)
        return current_plugin.__class__.__name__ == "NullTTS"

    def configure_audio_runtime(self):#20260716_kpopmodder
        if self.is_null_tts_provider():
            log_print("[TTS] audio runtime setup skipped for NullTTS provider")
            return

        ffmpeg_ready = ensure_ffmpeg_exists()#20260616_kpopmodder
        if not ffmpeg_ready:#20260616_kpopmodder
            log_print("[TTS] ffmpeg setup failed or ffmpeg is not available.")

    def configure_stop_hotkey_controls(self):#20260716_kpopmodder
        if self.is_null_tts_provider():
            log_print("[TTS] stop hotkey skipped for NullTTS provider")
            return

        self.register_stop_hotkey()#20260705_kpopmodder
        self.start_stop_hotkey_polling()#20260705_kpopmodder

    def register_stop_hotkey(self):#20260705_kpopmodder
        if keyboard is None:
            log_print("[TTS] stop hotkey disabled: keyboard module not available")
            return

        try:
            self.stop_hotkey_handle = keyboard.add_hotkey(
                self.stop_hotkey,
                self.on_stop_hotkey,
            )
            log_print(f"[TTS] stop hotkey registered: {self.stop_hotkey}")
        except Exception as e:
            log_print(f"[TTS] stop hotkey disabled: {e}")

    def unregister_stop_hotkey(self):#20260705_kpopmodder
        if keyboard is None or self.stop_hotkey_handle is None:
            return

        try:
            keyboard.remove_hotkey(self.stop_hotkey_handle)
        except Exception as e:
            log_print(f"[TTS] stop hotkey unregister failed: {e}")
        finally:
            self.stop_hotkey_handle = None

    def on_stop_hotkey(self):#20260705_kpopmodder
        now = time.time()
        if now - self.last_stop_hotkey_time < 0.6:
            return
        self.last_stop_hotkey_time = now
        log_print(f"[TTS] stop hotkey pressed: {self.stop_hotkey}")
        event_manager.trigger(EventType.INTERRUPT)

    def start_stop_hotkey_polling(self):#20260705_kpopmodder
        if self.stop_hotkey_poll_thread is not None:
            return
        try:
            user32 = ctypes.windll.user32
        except Exception as e:
            log_print(f"[TTS] stop hotkey polling disabled: {e}")
            return

        self.stop_hotkey_poll_thread = threading.Thread(
            target=self._stop_hotkey_poll_loop,
            args=(user32,),
            name="TTSStopHotkeyPoll",
            daemon=True,
        )
        self.stop_hotkey_poll_thread.start()
        log_print("[TTS] stop hotkey polling registered: ctrl+shift+alt+q")

    def _stop_hotkey_poll_loop(self, user32):#20260705_kpopmodder
        while not self.stop_hotkey_poll_stop_event.is_set():
            try:
                if self._is_stop_hotkey_down(user32):
                    self.on_stop_hotkey()
                    time.sleep(0.25)
                    continue
            except Exception as e:
                log_print(f"[TTS] stop hotkey polling failed: {e}")
                break
            time.sleep(0.05)

    def _is_stop_hotkey_down(self, user32):#20260705_kpopmodder
        vk_shift = 0x10
        vk_control = 0x11
        vk_menu = 0x12
        vk_q = 0x51
        return all(
            bool(user32.GetAsyncKeyState(vk) & 0x8000)
            for vk in (vk_shift, vk_control, vk_menu, vk_q)
        )

    def stop_stop_hotkey_polling(self):#20260705_kpopmodder
        self.stop_hotkey_poll_stop_event.set()
        thread = self.stop_hotkey_poll_thread
        if (
            thread is not None
            and thread.is_alive()
            and threading.current_thread() != thread
        ):
            thread.join(timeout=0.3)
        self.stop_hotkey_poll_thread = None

    # def clear_queues(self):#20260617_kpopmodder
    #     try:
    #         with self.queue_lock:
    #             self.clear_queue(self.input_queue)
    #             #self.clear_queue(self.audio_data_queue)#20260616_kpopmodder

    #     except Exception as e:
    #         log_print(f"[TTS interrupt] queue clear error: {e}")

    # def clear_queue(self, queue):#20260617_kpopmodder
    #     while True:
    #         try:
    #             queue.get_nowait()
    #         except Empty:
    #             break

    # def close_mouth_safely(self):#20260617_kpopmodder
    #     try:
    #         self.send_output(0)

    #     except Exception as e:
    #         log_print(f"[TTS interrupt] mouth close output error: {e}")

    # def join_audio_thread_safely(self):#20260617_kpopmodder
    #     try:
    #         if (
    #             self.audio_process_thread
    #             and self.audio_process_thread.is_alive()
    #             and threading.current_thread() != self.audio_process_thread
    #         ):
    #             self.audio_process_thread.join(timeout=0.3)

    #     except Exception as e:
    #         log_print(f"[TTS interrupt] join error: {e}")

    # def send_output(self, output):#20260617_kpopmodder
    #     for subscriber in self.output_event_listeners:#20260617_kpopmodder
    #         #subcriber(output)#20260617_kpopmodder
    #         try:#20260617_kpopmodder
    #             subscriber (output)#20260617_kpopmodder
    #         except Exception as e:
    #             log_print(f"[TTS output listener error] {e}")

    def send_output(self, output):#20260617_kpopmodder
        if getattr(self, "disable_mouth_output", False):#20260617_kpopmodder#VtubeStudio 입 움직임 차단 추가
            return

        try:
            output = float(output)
        except Exception:
            output = 0.0

        now = time.time()

        # 입 움직임 값은 너무 자주 보내지 않음.
        # 단, 0은 입 닫기 값이라 항상 보냄.
        if output != 0 and now - self.last_output_send_time < self.output_min_interval:
            return

        self.last_output_send_time = now

        with self.output_lock:
            for subscriber in list(self.output_event_listeners):
                try:
                    subscriber(output)
                except Exception as e:
                    log_print(f"[TTS output listener error] {e}")

    def add_output_event_listener(self, function):
        if function in self.output_event_listeners:
            return
        self.output_event_listeners.append(function)

    def remove_output_event_listener(self, function):
        removed = False
        while function in self.output_event_listeners:
            self.output_event_listeners.remove(function)
            removed = True
        return removed

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        #20260623_kpopmodder: Reuse interrupt cleanup for shutdown so audio/mouth state exits safely.
        try:
            self.interrupt_controller.handle_interrupt()
        except Exception as e:
            log_print(f"[TTS shutdown] interrupt cleanup error: {e}")

        if self._interrupt_subscription is not None:
            self._interrupt_subscription.unsubscribe()
            self._interrupt_subscription = None

        self.unregister_stop_hotkey()#20260705_kpopmodder
        self.stop_stop_hotkey_polling()#20260705_kpopmodder

        self.output_event_listeners.clear()
        super().shutdown()

    def unpack_input_payload(self, payload):#20260623_kpopmodder
        if not isinstance(payload, dict) or "text" not in payload:
            return payload, None

        response_generation = payload.get("response_generation")
        try:
            response_generation = int(response_generation)
        except (TypeError, ValueError):
            response_generation = None
        return payload.get("text"), response_generation

    def update_latest_response_generation(self, response_generation):#20260623_kpopmodder
        if response_generation is None:
            return False
        with self.response_generation_lock:
            if (
                self.latest_response_generation is None
                or response_generation > self.latest_response_generation
            ):
                self.latest_response_generation = response_generation
                return True
        return False

    def get_latest_response_generation(self):#20260623_kpopmodder
        with self.response_generation_lock:
            return self.latest_response_generation

    def is_stale_response_generation(self, response_generation):#20260623_kpopmodder
        if response_generation is None:
            return False
        latest_response_generation = self.get_latest_response_generation()
        return (
            latest_response_generation is not None
            and response_generation < latest_response_generation
        )

    def make_queue_item(
        self,
        queue_generation,
        text,
        response_generation=None,
    ):#20260623_kpopmodder
        if response_generation is None:
            return (queue_generation, text)
        return {
            "queue_generation": queue_generation,
            "response_generation": response_generation,
            "text": text,
        }

    def parse_queue_item(self, item, default_queue_generation=None):#20260623_kpopmodder
        if isinstance(item, dict):
            return (
                item.get("queue_generation", default_queue_generation),
                item.get("response_generation"),
                item.get("text", ""),
            )

        if isinstance(item, tuple):
            if len(item) == 3:
                return item
            if len(item) == 2:
                queue_generation, text = item
                return queue_generation, None, text

        return default_queue_generation, None, item

    def drop_queued_older_response_items(self, min_response_generation):#20260623_kpopmodder
        kept_items = []
        dropped_count = 0

        while True:
            try:
                item = self.input_queue.get_nowait()
            except Empty:
                break

            _, response_generation, _ = self.parse_queue_item(item)
            if (
                response_generation is not None
                and response_generation < min_response_generation
            ):
                dropped_count += 1
                continue
            kept_items.append(item)

        for item in kept_items:
            self.input_queue.put(item)

        return dropped_count

    def get_queue_generation(self):#20260621_kpopmodder
        with self.queue_generation_lock:
            return self.queue_generation


    def bump_queue_generation(self):#20260621_kpopmodder
        with self.queue_generation_lock:
            self.queue_generation += 1
            return self.queue_generation


    def get_queue_display_items(self):#20260621_kpopmodder
        try:
            raw_items = list(self.input_queue.queue)
        except Exception:
            return []
        display_items = []
        for item in raw_items:
            _, _, text = self.parse_queue_item(item)#20260623_kpopmodder
            display_items.append(text)
        return display_items

# import io
# import os
# from queue import Queue, Empty#20260613_kpopmodder
# import shutil
# import threading
# import zipfile
# import numpy as np

# import requests
# from tqdm import tqdm
# from ui_core.live_textbox import LiveTextbox
# from plugin_system.interfaces import TTSPluginInterface
# import gradio as gr
# from plugin_system.selection import PluginSelectionBase
# import LAV_utils
# from pydub import AudioSegment
# import simpleaudio as sa
# #import pyaudio#20260614_kpopmodder
# from pydub import AudioSegment
# from pydub.utils import audioop
# from core.event_manager import event_manager, EventType
# from core.global_state import global_state, GlobalKeys#20260611_kpopmodder
# import time#20260611_kpopmodder
# from core.logger import log_print, debug_print#20260612_kpopmodder
# from safety_filter import clean_text#20260613_kpopmodder
# from audio_device_manager import audio_device_manager#20260614_kpopmodder

# #DEBUG_TTS_INTERRUPT  = False#20260611_kpopmodder

# class TTS(PluginSelectionBase):
#     output_event_listeners = []

# #     input_queue = Queue()#20260613_kpopmodder
# #     audio_data_queue = Queue()#20260613_kpopmodder
# #     audio_process_thread = None#20260613_kpopmodder
# #     audio_playback_thread = None#20260613_kpopmodder
# # #    interrupt = False
# #     interrupt_event = threading.Event()#20260613_kpopmodder

#     log_live_textbox = LiveTextbox()
#     process_queue_live_textbox = LiveTextbox()
#     playback_queue_live_textbox = LiveTextbox()

#     subtitle_file_path = "subtitle.txt"
#     def __init__(self) -> None:
#         super().__init__(TTSPluginInterface)

#         self.input_queue = Queue()#20260613_kpopmodder
#         self.audio_data_queue = Queue()#20260613_kpopmodder
#         self.audio_process_thread = None#20260613_kpopmodder
#         self.audio_playback_thread = None#20260613_kpopmodder
#         self.interrupt_event = threading.Event()#20260613_kpopmodder
#         self.queue_lock = threading.Lock()#20260613_kpopmodder
#         #self.audio_lock = threading.Lock()#20260614_kpopmodder
#         self.audio_lock = threading.RLock()#20260614_kpopmodder
#         self.synth_lock = threading.Lock()#20260614_kpopmodder
#         self.audio_device_manager = audio_device_manager#20260614_kpopmodder
        
#         self.check_ffmpeg()

#         event_manager.subscribe(EventType.INTERRUPT, self.handle_interrupt )

#     def create_ui(self):
#         with gr.Tab("TTS"):
#             super().create_plugin_selection_ui()
#             self.main_interface = gr.Interface(
#                 fn=self.wrapper_synthesize,
#                 inputs=[gr.Textbox(label="Original Text")],
#                 outputs=[gr.Audio(label="Synthesized Voice")],
#                 allow_flagging="never",
#                 examples=["すぅ…はぁ——おはようさん、朝の空気は清々しくて気持ちええなぁ、深呼吸して頭もすっきりや。",
#                           "金魚飼ったことある？大人しゅうて、めっちゃ可愛ええんや。",
#                           "全身ポカポカで気持ちええわぁ～、浮いとるみたい。",
#                           "Ah... *yawns* Good morning. The morning air is the freshest. Come on, take a few extra breaths — it'll make you smarter~",
#                           "Have you ever kept goldfish as pets? They're very cute.",
#                           "Ah, this is great! I feel so relaxed all over, I could almost float away.",
#                           "hello"]
#             )
#             gr.Markdown(
#                 "Note: Some prividers may only support certain languages.")
#             with gr.Accordion("Console", open=False):
#                 self.log_live_textbox.create_ui()
#                 self.process_queue_live_textbox.create_ui(
#                     lines=3, max_lines=3, label="Input waiting to be processed: ")
#                 self.playback_queue_live_textbox.create_ui(
#                     lines=3, max_lines=3, label="Generated audio waiting to be played: ")
            
#             super().create_plugin_ui()

#     def wrapper_synthesize(self, text):
#         text = clean_text(text)#20260613_kpopmodder

#         result = self.current_plugin.synthesize(text)
#         self.update_subtitle_file(text)
#         self.play_sound_from_bytes(result)
#         return result

#     VOICE_OUTPUT_FILENAME = "synthesized_voice.wav"

#     def receive_input(self, text):
#         if isinstance(text, list):
#             if all(isinstance(item, str) for item in text):#20260614_kpopmodder
#                 merged_items = []
#                 buffer = ""

#                 for item in text:#20260614_kpopmodder
#                     item = item.strip()
#                     item = clean_text(item)

#                     if item == "":
#                         continue

#                     if buffer:
#                         buffer += " " + item
#                     else:
#                         buffer = item

#                     if len(buffer) >= 15:#20260614_kpopmodder#짧은 문장들을 하나로 묶어서 TTS 요청 수를 줄이는 병합 로직 글자수
#                         merged_items.append(buffer)
#                         buffer = ""

#                 if buffer:#20260614_kpopmodder
#                     merged_items.append(buffer)

#                 with self.queue_lock:#20260614_kpopmodder
#                     for item in merged_items:
#                         self.input_queue.put(item)

#             else:#20260614_kpopmodder
#                 log_print("TTS: The list must contain only strings.")
#                 return

# #             # Check if every item in the list is a string
# #             if all(isinstance(item, str) for item in text):
# #                 for item in text:
# #                     item = item.strip()#20260613_kpopmodder
# #                     item = clean_text(item)#20260613_kpopmodder

# # #                    # 너무 짧은 문장 스킵 (숫자, 이모지, 단일 기호 방지)#20260613_kpopmodder
# #                     # if len(item) < 8:#20260613_kpopmodder
# #                     #     continue
# #                     with self.queue_lock:#20260613_kpopmodder
# #                         self.input_queue.put(item+"。")
# #             else:
# #                 log_print("TTS: The list must contain only strings.")#20260612_kpopmodder
# #                 return
#             # Check if the input is a string
#         elif isinstance(text, str):#20260614_kpopmodder
#             text = clean_text(text)

#             if text == "":
#                 log_print("TTS: ignoring empty input")
#                 return

#             split_items = self.split_tts_sentences(text)#20260614_kpopmodder

#             with self.queue_lock:#20260614_kpopmodder
#                 for item in split_items:
#                     self.input_queue.put(item)
#         # elif isinstance(text, str):#20260614_kpopmodder
#         #     text = clean_text(text)#20260613_kpopmodder

#         #     if text == "":
#         #         log_print("TTS: ignoring empty input")#20260612_kpopmodder
#         #         return
#         #     with self.queue_lock:#20260613_kpopmodder
#         #         self.input_queue.put(text)
#         #self.input_queue.put(text)
#         self.process_input_queue(self.current_plugin.synthesize)

#     def process_input_queue(self, function):  # 20260615_kpopmodder
#         def generate_audio():
#             global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)
#             debug_print("[TTS QUEUE] IS_AI_SPEAKING=True")

#             try:
#                 while True:
#                     with self.queue_lock:
#                         if self.input_queue.empty():
#                             break

#                         input_text = self.input_queue.get()

#                     input_text = input_text.strip()

#                     if not input_text:
#                         continue

#                     if self.interrupt_event.is_set():
#                         log_print("[TTS QUEUE] interrupt detected before synthesize")
#                         break

#                     log_print(f"[TTS QUEUE] start sentence: {input_text}")

#                     audio_result = None

#                     for retry_count in range(3):
#                         try:
#                             with self.synth_lock:
#                                 audio_result = function(input_text)

#                             if audio_result is not None:
#                                 break

#                             log_print(
#                                 f"[TTS QUEUE] synthesize returned None. retry={retry_count + 1}/3 "
#                                 f"text={input_text}"
#                             )

#                         except Exception as e:
#                             log_print(
#                                 f"[TTS QUEUE] synthesize failed. retry={retry_count + 1}/3 "
#                                 f"text={input_text}, error={e}"
#                             )

#                         time.sleep(1.0)

#                     if audio_result is None:
#                         log_print(f"[TTS QUEUE] synthesize finally failed after retries: {input_text}")
#                         continue

#                     if self.interrupt_event.is_set():
#                         log_print("[TTS QUEUE] interrupt detected after synthesize")
#                         break

#                     self.update_subtitle_file(input_text)

#                     playback_ok = False

#                     for retry_count in range(2):
#                         try:
#                             with self.audio_lock:
#                                 self.play_sound_from_bytes(audio_result)

#                             playback_ok = True
#                             break

#                         except Exception as e:
#                             log_print(
#                                 f"[TTS QUEUE] playback failed. retry={retry_count + 1}/2 "
#                                 f"text={input_text}, error={e}"
#                             )

#                         time.sleep(0.5)

#                     if not playback_ok:
#                         log_print(f"[TTS QUEUE] playback finally failed. force skip: {input_text}")
#                         continue

#                     log_print(f"[TTS QUEUE] finished sentence: {input_text}")

#                     self.process_queue_live_textbox.set(
#                         LAV_utils.queue_to_list(self.input_queue)
#                     )

#             except Exception as e:
#                 log_print(f"[TTS process_input_queue error] {e}")

#             finally:
#                 self.interrupt_event.clear()

#                 global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)
#                 global_state.set_value(
#                     GlobalKeys.LAST_AI_SPEAK_END_TIME,
#                     time.time()
#                 )
#                 debug_print("[TTS QUEUE] IS_AI_SPEAKING=False")

#         if self.audio_process_thread is None or not self.audio_process_thread.is_alive():
#             self.audio_process_thread = threading.Thread(target=generate_audio)
#             self.audio_process_thread.daemon = True
#             self.audio_process_thread.start()

#     # def process_input_queue(self, function):#20260614_kpopmodder
#     #     def generate_audio():
#     #         #while (not self.input_queue.empty()):
#     #         while True:#20260613_kpopmodder
#     #             # generate audio data and queue up for playing
#     #             with self.queue_lock:#20260613_kpopmodder
#     #                 if self.input_queue.empty():#20260613_kpopmodder
#     #                     break#20260613_kpopmodder
#     #                 input = self.input_queue.get()
                
#     #             while self.audio_data_queue.qsize() >= 1:#20260614_kpopmodder#TTS 생성 스레드가 재생보다 너무 앞서가지 못하게 제한
#     #                 if self.interrupt_event.is_set():#20260614_kpopmodder
#     #                     self.interrupt_event.clear()#20260614_kpopmodder
#     #                     return
#     #                 time.sleep(0.1)

#     #             with self.synth_lock:#20260614_kpopmodder
#     #                 audio_result = function(input)#20260613_kpopmodder

#     #             with self.queue_lock:#20260613_kpopmodder
#     #                 #self.audio_data_queue.put([function(input), input])
#     #                 self.audio_data_queue.put([audio_result, input])#20260613_kpopmodder
#     #             self.process_audio_queue(self.play_sound_from_bytes)
#     #             self.process_queue_live_textbox.set(
#     #                 LAV_utils.queue_to_list(self.input_queue))
#     #             self.log_live_textbox.print(f"Audio synthesized for: {input}")

#     #     # Check if the current thread is alive
#     #     if self.audio_process_thread is None or not self.audio_process_thread.is_alive():
#     #         # Create and start a new thread
#     #         self.audio_process_thread = threading.Thread(target=generate_audio)
#     #         self.audio_process_thread.daemon = True#20260614_kpopmodder
#     #         self.audio_process_thread.start()

#     def update_subtitle_file(self, text):
#         with open(self.subtitle_file_path, 'w', encoding='utf-8') as file:
#             file.write(text)

#     def process_audio_queue(self, function):
#         return  # 20260614_kpopmodder - disabled. audio is played synchronously in process_input_queue
    
#         def play_audio():
#             global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)#20260611_kpopmodder

# #            if DEBUG_TTS_INTERRUPT:#20260611_kpopmodder
#             debug_print("[TTS QUEUE] IS_AI_SPEAKING=True")#20260612_kpopmodder

#             try:#20260611_kpopmodder
#                 #while (not self.audio_data_queue.empty()):
#                 while True:#20260613_kpopmodder
#                     # generate audio data and queue up for playing
#                     # with self.queue_lock:#20260613_kpopmodder
#                     #     if self.audio_data_queue.empty():#20260613_kpopmodder
#                     #         break#20260613_kpopmodder
#                     #     audio_data_pair = self.audio_data_queue.get()
#                     try:#20260614_kpopmodder
#                         audio_data_pair = self.audio_data_queue.get(timeout=1.0)
#                     except Empty:#20260614_kpopmodder
#                         break
#                     self.update_subtitle_file(audio_data_pair[1])
#                     with self.audio_lock:#20260614_kpopmodder
#                         function(audio_data_pair[0])
#                     self.playback_queue_live_textbox.set(
#                         LAV_utils.queue_to_list(self.audio_data_queue))
#             finally:#20260611_kpopmodder
#                 global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)

#                 global_state.set_value(#20260611_kpopmodder
#                     GlobalKeys.LAST_AI_SPEAK_END_TIME,
#                     time.time()
#                 )

# #                if DEBUG_TTS_INTERRUPT:#20260611_kpopmodder
#                 debug_print("[TTS QUEUE] IS_AI_SPEAKING=False")#20260612_kpopmodder

#         # Check if the current thread is alive
#         if self.audio_playback_thread is None or not self.audio_playback_thread.is_alive():
#             # Create and start a new thread
#             self.audio_playback_thread = threading.Thread(target=play_audio)
#             self.audio_playback_thread.daemon = True#20260614_kpopmodder
#             self.audio_playback_thread.start()

#     def find_max_rms(self, audio_segment, chunk_size=1024):
#         """
#         Find the maximum RMS value in the given audio segment.
#         """
#         max_rms = 0
#         for i in range(0, len(audio_segment.raw_data), chunk_size):
#             chunk_data = audio_segment.raw_data[i:i+chunk_size]
#             rms = audioop.rms(chunk_data, audio_segment.sample_width)
#             if rms > max_rms:
#                 max_rms = rms
#         return max_rms

#     # def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260611_kpopmodder
#     #     """
#     #     Play audio from bytes and normalize volume in real time, with improved synchronization.
#     #     """
#     #     if(audio_data == None): return
#     #     # Open the audio data with PyDub
#     #     audio = AudioSegment.from_file(
#     #         io.BytesIO(audio_data), format="wav")

#     #     # Find the maximum RMS value for normalization
#     #     max_rms = self.find_max_rms(audio, chunk_size)

#     #     p = pyaudio.PyAudio()

#     #     try:
#     #         stream = p.open(format=p.get_format_from_width(audio.sample_width),
#     #                         channels=audio.channels,
#     #                         rate=audio.frame_rate,
#     #                         output=True,
#     #                         frames_per_buffer=chunk_size)
#     #     except OSError as e:
#     #         if e.errno == -9997:
#     #             log_print(f"Error: Invalid sample rate {audio.frame_rate}. Please check your audio device or adjust the rate.")
#     #         else:
#     #             log_print(f"Unexpected error occurred: {e}")
#     #     except Exception as e:
#     #         log_print(f"An error occurred: {e}")

#     #     def process_chunk(i):
#     #         chunk_data = audio.raw_data[i:i+chunk_size]
#     #         rms = audioop.rms(chunk_data, audio.sample_width)
#     #         normalized_volume = rms / max_rms
#     #         return chunk_data, normalized_volume

#     #     # Initial volume calculation for the first chunk
#     #     chunk_data, normalized_volume = process_chunk(0)
#     #     # Process and play audio in chunks
#     #     for i in range(chunk_size, len(audio.raw_data), chunk_size):
#     #         # Play the current chunk
#     #         stream.write(chunk_data)

#     #         if (self.interrupt): 
#     #             self.input_queue = Queue()
#     #             self.audio_data_queue = Queue()
#     #             self.interrupt = False
#     #             # Stop and close the stream
#     #             stream.stop_stream()
#     #             stream.close()
#     #             # Close PyAudio
#     #             p.terminate()
#     #             break
#     #         # Calculate volume for the next chunk
#     #         chunk_data, normalized_volume = process_chunk(i)
#     #         self.send_output(normalized_volume)
#     #         # log_print(f"Normalized Volume: {normalized_volume}")

#     #     # Play the last chunk
#     #     stream.write(chunk_data)

#     #     # Stop and close the stream
#     #     stream.stop_stream()
#     #     stream.close()

#     #     # Close PyAudio
#     #     p.terminate()

# #     def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260614_kpopmodder
# #         if audio_data is None:
# #             return

# # #        global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)
# # #        log_print("[TTS] IS_AI_SPEAKING=True")#20260611_kpopmodder

# #         p = None#20260614_kpopmodder
# #         stream = None#20260614_kpopmodder

# #         try:
# #             audio = AudioSegment.from_file(io.BytesIO(audio_data), format="wav")
# #             max_rms = self.find_max_rms(audio, chunk_size)
# #             if max_rms <= 0:
# #                 return

# #             p = pyaudio.PyAudio()

# #             try:#20260614_kpopmodder
# #                 default_info = p.get_default_output_device_info()
# #                 log_print(f"[TTS playback] default output: {default_info.get('name')}")
# #             except Exception as e:#20260614_kpopmodder
# #                 log_print(f"[TTS playback] default output query failed: {e}")
# #             # stream = p.open(#20260614_kpopmodder
# #             #     format=p.get_format_from_width(audio.sample_width),
# #             #     channels=audio.channels,
# #             #     rate=audio.frame_rate,
# #             #     output=True,
# #             #     output_device_index=self.audio_device_manager.output_device_id,#20260614_kpopmodder
# #             #     frames_per_buffer=chunk_size
# #             # )

# #             open_kwargs = dict(#20260614_kpopmodder
# #                 format=p.get_format_from_width(audio.sample_width),
# #                 channels=audio.channels,
# #                 rate=audio.frame_rate,
# #                 output=True,
# #                 frames_per_buffer=chunk_size,
# #                 start=False#20260614_kpopmodder
# #             )

# #             # if self.audio_device_manager.output_device_id is not None:#20260614_kpopmodder
# #             #     open_kwargs["output_device_index"] = self.audio_device_manager.output_device_id

# #             # stream = p.open(**open_kwargs)

# #             #output_device_id = self.audio_device_manager.output_device_id#20260614_kpopmodder
# #             output_device_id = self.audio_device_manager.get_output_device_id()#20260614_kpopmodder

# #             if output_device_id is not None:#20260614_kpopmodder
# #                 try:
# #                     info = p.get_device_info_by_index(output_device_id)#20260614_kpopmodder

# #                     if info.get("maxOutputChannels", 0) > 0:#20260614_kpopmodder
# #                         #open_kwargs["output_device_index"] = output_device_id#20260614_kpopmodder
# #                         #log_print(f"[TTS playback] using output device: {output_device_id}: {info.get('name')}")#20260614_kpopmodder

# #                         # PyAudio에서 USB 오디오 장치를 직접 지정하면#20260614_kpopmodder
# #                         # PortAudio C-level crash가 날 수 있어서 안정성 우선으로 기본 출력만 사용#20260614_kpopmodder
# #                         # open_kwargs["output_device_index"] = output_device_id#20260614_kpopmodder

# #                         log_print(#20260614_kpopmodder
# #                             f"[TTS playback] selected output device ignored for stability: "
# #                             f"{output_device_id}: {info.get('name')}"
# #                         )
# #                     else:
# #                         log_print(f"[TTS playback] device is not output device: {output_device_id}")
# #                         output_device_id = None

# #                 except Exception as e:#20260614_kpopmodder
# #                     log_print(f"[TTS playback] invalid output device={output_device_id}, fallback to default: {e}")
# #                     output_device_id = None

# #             try:
# #                 stream = p.open(**open_kwargs)

# #             except Exception as e:#20260614_kpopmodder
# #                 log_print(f"[TTS playback] open failed with selected device. fallback to default: {e}")

# #                 open_kwargs.pop("output_device_index", None)

# #                 try:
# #                     stream = p.open(**open_kwargs)
# #                 except Exception as e2:#20260614_kpopmodder
# #                     log_print(f"[TTS playback] default output also failed. skip playback: {e2}")#20260614_kpopmodder
# #                     return
                
# #             try:#20260614_kpopmodder
# #                 stream.start_stream()
# #             except Exception as e:#20260614_kpopmodder
# #                 log_print(f"[TTS playback] start_stream error: {e}")
# #                 return

# #             try:
# #                 if stream is None:#20260614_kpopmodder
# #                     log_print("[TTS playback] stream is None. abort.")
# #                     return
                
# #                 for i in range(0, len(audio.raw_data), chunk_size):
# #                     chunk_data = audio.raw_data[i:i + chunk_size]

# #                     # if self.interrupt:#20260613_kpopmodder
# #                     #     self.input_queue = Queue()
# #                     #     self.audio_data_queue = Queue()
# #                     #     self.interrupt = False
# #                     #     break

# #                     if self.interrupt_event.is_set():#20260614_kpopmodder
# #                         with self.queue_lock:
# #                             while True:
# #                                 try:#20260614_kpopmodder
# #                                     self.input_queue.get_nowait()
# #                                 except Empty:
# #                                     break

# #                             while True:
# #                                 try:#20260614_kpopmodder
# #                                     self.audio_data_queue.get_nowait()
# #                                 except Empty:
# #                                     break

# #                         self.interrupt_event.clear()#20260614_kpopmodder
# #                         self.send_output(0)#20260614_kpopmodder
# #                         return#20260614_kpopmodder
# #                         # with self.queue_lock:#20260613_kpopmodder
# #                         #     while True:
# #                         #         try:
# #                         #             self.input_queue.get_nowait()
# #                         #         except Empty:
# #                         #             break

# #                         #     while True:
# #                         #         try:
# #                         #             self.audio_data_queue.get_nowait()
# #                         #         except Empty:
# #                         #             break

# #                         # self.interrupt_event.clear()
# #                         # break#20260613_kpopmodder

# #                     #with self.audio_lock:#20260614_kpopmodder
# #                     #stream.write(chunk_data)#20260614_kpopmodder
# #                     # try:#20260614_kpopmodder
# #                     #     #stream.write(chunk_data, exception_on_underflow=False)#20260614_kpopmodder
# #                     #     if not stream.is_active():#20260614_kpopmodder
# #                     #         log_print("[TTS playback] stream inactive. stop playback.")
# #                     #         break

# #                     #     try:
# #                     #         stream.write(chunk_data)#20260614_kpopmodder
# #                     #     except OSError as e:#20260614_kpopmodder
# #                     #         log_print(f"[TTS playback] OSError during write: {e}")
# #                     #         break
# #                     #     except Exception as e:#20260614_kpopmodder
# #                     #         log_print(f"[TTS playback] stream.write error: {e}")
# #                     #         break
# #                     # except Exception as e:#20260614_kpopmodder
# #                     #     log_print(f"[TTS playback] stream.write error ignored: {e}")
# #                     #     break

# #                     try:#20260614_kpopmodder
# #                         if not stream.is_active():
# #                             log_print("[TTS playback] stream inactive. stop playback.")
# #                             break

# #                         stream.write(chunk_data)

# #                     except OSError as e:#20260614_kpopmodder
# #                         log_print(f"[TTS playback] OSError during write: {e}")
# #                         break

# #                     except Exception as e:#20260614_kpopmodder
# #                         log_print(f"[TTS playback] stream.write error: {e}")
# #                         break

# #                     rms = audioop.rms(chunk_data, audio.sample_width)
# #                     normalized_volume = rms / max_rms
# #                     self.send_output(normalized_volume)

# #             # finally:
# #             #     self.send_output(0)#20260614_kpopmodder

# #             #     stream.stop_stream()
# #             #     stream.close()
# #             #     p.terminate()

# #             # finally:#20260614_kpopmodder
# #             #     self.send_output(0)#20260614_kpopmodder

# #             #     #with self.audio_lock:#20260614_kpopmodder
# #             #     # if stream is not None:#20260614_kpopmodder
# #             #     #     try:#20260614_kpopmodder
# #             #     #         stream.stop_stream()#20260614_kpopmodder
# #             #     with self.audio_lock:#20260614_kpopmodder
# #             #         if stream is not None:
# #             #             try:
# #             #                 if stream.is_active():#20260614_kpopmodder
# #             #                     stream.stop_stream()
# #             #             except Exception as e:
# #             #                 log_print(f"[TTS playback] stop_stream error: {e}")#20260614_kpopmodder

# #             #             try:
# #             #                 stream.close()
# #             #             except Exception as e:
# #             #                 log_print(f"[TTS playback] stream close error: {e}")#20260614_kpopmodder

# #             #             stream = None

# #             #     if p is not None:#20260614_kpopmodder
# #             #         try:
# #             #             p.terminate()
# #             #         except Exception as e:#20260614_kpopmodder
# #             #             log_print(f"[TTS playback] pyaudio terminate error: {e}")
# #             #         # except Exception as e:#20260614_kpopmodder
# #             #         #     log_print(f"[TTS playback] stop_stream error: {e}")

# #             #         try:
# #             #             stream.close()
# #             #         except Exception as e:
# #             #             log_print(f"[TTS playback] stream close error: {e}")

# #             #     if p is not None:#20260614_kpopmodder
# #             #         try:
# #             #             p.terminate()
# #             #         except Exception as e:
# #             #             log_print(f"[TTS playback] pyaudio terminate error: {e}")

# #             finally:#20260614_kpopmodder
# #                 self.send_output(0)#20260614_kpopmodder

# #                 with self.audio_lock:#20260614_kpopmodder
# #                     if stream is not None:
# #                         try:
# #                             if stream.is_active():
# #                                 stream.stop_stream()
# #                         except Exception as e:
# #                             log_print(f"[TTS playback] stop_stream error: {e}")

# #                         try:
# #                             stream.close()
# #                         except Exception as e:
# #                             log_print(f"[TTS playback] stream close error: {e}")

# #                         stream = None

# #                 if p is not None:#20260614_kpopmodder
# #                     try:
# #                         p.terminate()
# #                     except Exception as e:#20260614_kpopmodder
# #                         log_print(f"[TTS playback] pyaudio terminate error: {e}")

# #         # except Exception as e:#20260611_kpopmodder
# #         #     log_print(f"[TTS playback error] {e}")#20260612_kpopmodder

# #         except Exception as e:#20260614_kpopmodder
# #             import traceback#20260614_kpopmodder
# #             log_print(f"[TTS playback error] {e}")
# #             log_print(traceback.format_exc())#20260614_kpopmodder

# #             with self.queue_lock:
# #                 while True:
# #                     try:
# #                         self.input_queue.get_nowait()
# #                     except Empty:
# #                         break

# #                 while True:
# #                     try:
# #                         self.audio_data_queue.get_nowait()
# #                     except Empty:
# #                         break

# #             self.interrupt_event.clear()
# #             global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)
# #             global_state.set_value(GlobalKeys.LAST_AI_SPEAK_END_TIME, time.time())

# #             try:
# #                 self.send_output(0)
# #             except Exception:
# #                 pass

# #             time.sleep(1.0)
# #             return#20260614_kpopmodder

# # #        finally:
# # #            global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)#20260611_kpopmodder
# # #            log_print("[TTS] IS_AI_SPEAKING=False")#20260611_kpopmodder

#     def play_sound_from_bytes(self, audio_data, chunk_size=1024):#20260614_kpopmodder
#         if audio_data is None:
#             return

#         play_obj = None

#         try:
#             audio = AudioSegment.from_file(io.BytesIO(audio_data), format="wav")
#             max_rms = self.find_max_rms(audio, chunk_size)

#             if max_rms <= 0:
#                 return

#             log_print("[TTS playback] using simpleaudio for stability")

#             play_obj = sa.play_buffer(
#                 audio.raw_data,
#                 num_channels=audio.channels,
#                 bytes_per_sample=audio.sample_width,
#                 sample_rate=audio.frame_rate
#             )

#             for i in range(0, len(audio.raw_data), chunk_size):
#                 if self.interrupt_event.is_set():
#                     try:#20260615_kpopmodder
#                         if play_obj is not None:
#                             play_obj.stop()
#                     except Exception as e:#20260615_kpopmodder
#                         log_print(f"[TTS playback] simpleaudio stop error: {e}")

#                     self.send_output(0)#20260615_kpopmodder
#                     return
#                     # with self.queue_lock:#20260615_kpopmodder
#                     #     while True:
#                     #         try:
#                     #             self.input_queue.get_nowait()
#                     #         except Empty:
#                     #             break

#                     #     while True:
#                     #         try:
#                     #             self.audio_data_queue.get_nowait()
#                     #         except Empty:
#                     #             break

#                     # try:#20260615_kpopmodder
#                     #     if play_obj is not None:
#                     #         play_obj.stop()
#                     # except Exception as e:
#                     #     log_print(f"[TTS playback] simpleaudio stop error: {e}")

#                     # self.send_output(0)
#                     # return

#                 chunk_data = audio.raw_data[i:i + chunk_size]

#                 try:
#                     rms = audioop.rms(chunk_data, audio.sample_width)
#                     normalized_volume = rms / max_rms
#                     self.send_output(normalized_volume)
#                 except Exception as e:
#                     log_print(f"[TTS playback] volume meter error: {e}")

#                 time.sleep(chunk_size / audio.frame_rate / audio.channels / audio.sample_width)

#             try:
#                 if play_obj is not None:
#                     play_obj.wait_done()
#             except Exception as e:
#                 log_print(f"[TTS playback] simpleaudio wait_done error: {e}")

#         except Exception as e:
#             import traceback
#             log_print(f"[TTS playback error] {e}")
#             log_print(traceback.format_exc())

#         finally:
#             if self.interrupt_event.is_set():
#                 try:
#                     if play_obj is not None:
#                         play_obj.stop()
#                 except Exception:
#                     pass

#             self.send_output(0)

#     def check_ffmpeg(self):
#         # https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.7z

#         # Check if the VoicevoxEngine folder exists
#         if not os.path.exists("ffmpeg.exe"):
#             # Define the file name and path for the ZIP file
#             file_name = "ffmpeg-release-essentials.zip"

#             # URL to download the ZIP file
#             url = "https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-6.1.1-essentials_build.zip"

#             # Download the ZIP file with progress
#             log_print(f"Downloading {file_name} from {url}...")#20260612_kpopmodder
#             response = requests.get(url, stream=True)

#             if response.status_code == 200:
#                 total_size_in_bytes = int(
#                     response.headers.get('content-length', 0))
#                 block_size = 1024  # 1 Kibibyte

#                 progress_bar = tqdm(total=total_size_in_bytes,
#                                     unit='iB', unit_scale=True)
#                 with open(file_name, 'wb') as file:
#                     for data in response.iter_content(block_size):
#                         progress_bar.update(len(data))
#                         file.write(data)
#                 progress_bar.close()

#                 if total_size_in_bytes != 0 and progress_bar.n != total_size_in_bytes:
#                     log_print("ERROR, something went wrong during download")#20260612_kpopmodder
#                 else:
#                     log_print(f"{file_name} downloaded successfully.")#20260612_kpopmodder

#                 # Extract and rename the ZIP file contents
#                 log_print(f"Extracting {file_name}...")#20260612_kpopmodder
#                 with zipfile.ZipFile(file_name, 'r') as zip_ref:
#                     zip_ref.extractall()
#                 log_print(f"{file_name} extracted successfully.")#20260612_kpopmodder

#                 current_module_directory = os.path.dirname(__file__)
#                 # Path to the ffmpeg.exe inside the extracted folder
#                 ffmpeg_exe_path = os.path.join(
#                     current_module_directory, 'ffmpeg-6.1.1-essentials_build', 'bin', 'ffmpeg.exe')
#                 ffprobe_exe_path = os.path.join(
#                     current_module_directory, 'ffmpeg-6.1.1-essentials_build', 'bin', 'ffprobe.exe')

#                 # Move ffmpeg.exe to the base directory
#                 shutil.move(ffmpeg_exe_path, current_module_directory)
#                 shutil.move(ffprobe_exe_path, current_module_directory)

#                 # Delete the extracted folder
#                 shutil.rmtree('ffmpeg-6.1.1-essentials_build')

#                 # Delete the ZIP file after extraction
#                 os.remove(file_name)

#     def split_tts_sentences(self, text, max_len=80):  # 20260614_kpopmodder
#         import re

#         text = text.replace("\n", " ")
#         text = re.sub(r"\s+", " ", text).strip()

#         if not text:
#             return []

#         raw_sentences = re.split(r'(?<=[.!?。！？…])\s+', text)

#         result = []

#         for sentence in raw_sentences:
#             sentence = sentence.strip()

#             if not sentence:
#                 continue

#             while len(sentence) > max_len:
#                 cut = sentence[:max_len]

#                 split_pos = max(
#                     cut.rfind(","),
#                     cut.rfind(" "),
#                     cut.rfind("，")
#                 )

#                 if split_pos <= 0:
#                     split_pos = max_len

#                 result.append(sentence[:split_pos].strip())
#                 sentence = sentence[split_pos:].strip()

#             if sentence:
#                 result.append(sentence)

#         log_print(f"[TTS split] {result}")
#         return result

#     def handle_interrupt(self):
# #        self.interrupt = True
#         self.interrupt_event.set()#20260613_kpopmodder
#         log_print("Interrupting pipeline")#20260612_kpopmodder

#         try:#20260614_kpopmoddder
#             with self.queue_lock:#20260614_kpopmoddder
#                 while True:#20260614_kpopmoddder
#                     try:
#                         self.input_queue.get_nowait()#20260614_kpopmoddder
#                     except Empty:#20260614_kpopmoddder
#                         break

#                 while True:#20260614_kpopmoddder
#                     try:
#                         self.audio_data_queue.get_nowait()#20260614_kpopmoddder
#                     except Empty:
#                         break
#         except Exception as e:#20260614_kpopmoddder
#             log_print(f"[TTS interrupt] queue clear error: {e}")

#         try:#20260614_kpopmoddder
#             self.send_output(0)#20260614_kpopmoddder
#         except Exception as e:#20260614_kpopmoddder
#             log_print(f"[TTS interrupt] mouth close output error: {e}")#20260614_kpopmoddder
        
#         #time.sleep(0.1)#20260614_kpopmodder
#         try:#20260614_kpopmodder
#             #if self.audio_process_thread and self.audio_process_thread.is_alive():#20260614_kpopmodder
#             if (#20260614_kpopmodder
#                 self.audio_process_thread
#                 and self.audio_process_thread.is_alive()
#                 and threading.current_thread() != self.audio_process_thread
#             ):
#                 self.audio_process_thread.join(timeout=0.3)

#         except Exception as e:#20260614_kpopmodder
#             log_print(f"[TTS interrupt] join error: {e}")
#         finally:#20260614_kpopmodder
#             self.interrupt_event.clear()
    
#     def send_output(self, output):
#         # log_print(output)
#         for subcriber in self.output_event_listeners:
#             subcriber(output)

#     def add_output_event_listener(self, function):
#         self.output_event_listeners.append(function)
