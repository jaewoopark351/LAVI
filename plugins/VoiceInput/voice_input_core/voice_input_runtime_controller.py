#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
from threading import Lock, Thread, current_thread
import time

import gradio as gr

from core.global_state import global_state, GlobalKeys
from core.logger import log_print


class VoiceInputRuntimeController:#20260618_kpopmodder
    def __init__(
        self,
        state,
        liveTextbox,
        interrupt_controller,
        open_mic_controller
    ):
        self.state = state
        self.liveTextbox = liveTextbox
        self.interrupt_controller = interrupt_controller
        self.open_mic_controller = open_mic_controller
        self._listen_thread = None#20260628_kpopmodder: Keep the recording loop joinable during shutdown.
        self._listen_thread_lock = Lock()#20260628_kpopmodder

    def start_listening(self):
        if self.state.recording:
            self.liveTextbox.print("Already listening.")
            return self.liveTextbox.get_text()

        if self._is_listen_thread_alive():#20260628_kpopmodder: Avoid starting a second mic loop while the old listen call exits.
            self.liveTextbox.print("Listener is still stopping. Please wait.")
            return self.liveTextbox.get_text()

        gr.Info("starting listening...")

        self.state.recording = True

        thread = Thread(
            target=self.transcribe_loop,
            name="VoiceInputTranscribeLoop",
        )#20260628_kpopmodder
        thread.daemon = True
        with self._listen_thread_lock:
            self._listen_thread = thread
        thread.start()

        self.liveTextbox.print("Started listening...")
        return self.liveTextbox.get_text()

    def stop_listening(self):
        gr.Info("Stopping listening...")

        self.state.recording = False
        self.state.ambience_adjusted = False
        self._join_listen_thread(
            timeout=0.2,
            context="stop_listening",
        )#20260628_kpopmodder: UI stop should not block long while sounddevice listen unwinds.

        self.liveTextbox.print("Stopped listening...")
        return self.liveTextbox.get_text()

    def shutdown(self, join_timeout=3.5):#20260628_kpopmodder: Wait for the mic loop so app shutdown does not leave a dangling daemon thread.
        self.state.recording = False
        self.state.ambience_adjusted = False
        self._join_listen_thread(
            timeout=join_timeout,
            context="shutdown",
        )

    def transcribe_loop(self):
        try:
            while self.state.recording:
                try:
                    self.transcribe()

                except OSError as e:
                    log_print(f"[VoiceInput transcribe_loop OSError skipped] {e}")
                    time.sleep(1.0)

                except Exception as e:
                    log_print(f"[VoiceInput transcribe_loop error] {e}")
                    time.sleep(0.5)
        finally:
            self._clear_listen_thread_if_current()#20260628_kpopmodder

    def transcribe(self):
        if global_state.get_value(GlobalKeys.IS_SONG_PLAYING, False):
            time.sleep(0.2)#20260628_kpopmodder: Song playback is not user-interruptible by mic input.
            return

        if self._is_in_ai_cooldown():
            self.liveTextbox.print("AI speech cooldown. Mic ignored.")
            time.sleep(0.2)
            return

        if global_state.get_value(GlobalKeys.IS_AI_SPEAKING, False):
            self.interrupt_controller.handle_ai_speaking()
            return

        if self.state.mic_mode == "open mic":
            self.open_mic_controller.handle_open_mic()
            return

    def _is_in_ai_cooldown(self):
        last_ai_end = global_state.get_value(
            GlobalKeys.LAST_AI_SPEAK_END_TIME,
            0
        )
        return time.time() - last_ai_end < 0.3

    def _is_listen_thread_alive(self):
        with self._listen_thread_lock:
            thread = self._listen_thread
        return thread is not None and thread.is_alive()

    def _join_listen_thread(self, timeout, context):
        with self._listen_thread_lock:
            thread = self._listen_thread

        if thread is None or not thread.is_alive():
            return
        if thread is current_thread():
            return

        thread.join(timeout=max(0.0, float(timeout or 0.0)))

        if thread.is_alive():
            log_print(
                "[VoiceInput] recording thread still stopping after "
                f"{context} join timeout={timeout}"
            )#20260628_kpopmodder
            return

        with self._listen_thread_lock:
            if self._listen_thread is thread:
                self._listen_thread = None

    def _clear_listen_thread_if_current(self):
        with self._listen_thread_lock:
            if self._listen_thread is current_thread():
                self._listen_thread = None
