import threading

import gradio as gr

from core.event_manager import event_manager, EventType
from core.logger import log_print
from plugins.SongPlayer.song_player_core.song_manifest import SongManifest
from plugins.SongPlayer.song_player_core.song_playback_controller import (
    SongPlaybackController,
)


class SongPlayer:#20260628_kpopmodder
    def __init__(self):
        self.manifest = SongManifest()
        self.manifest.load()
        self.output_event_listeners = []
        self.expression_event_listeners = []#20260628_kpopmodder
        #20260629_kpopmodder: Guard listener lists across playback callbacks, Stop, and shutdown.
        self.output_lock = threading.RLock()
        self.expression_lock = threading.RLock()
        self.last_status = self.manifest.status_text()
        self.playback_controller = SongPlaybackController(
            plugin_root=self.manifest.plugin_root,
            output_callback=self.send_output,
            expression_callback=self.send_expression,
            status_callback=self.set_status,
        )
        #20260628_kpopmodder: Songs are stopped only by Stop, new Play, or shutdown.
        self._shutdown = False

    def create_ui(self):
        choices = self.manifest.get_titles()

        with gr.Tab("Song Player"):
            self.song_dropdown = gr.Dropdown(
                label="Song",
                choices=choices,
                value=self.manifest.get_initial_title(),
                interactive=True,
            )

            with gr.Row():
                self.play_button = gr.Button("Play")
                self.stop_button = gr.Button("Stop")
                self.refresh_button = gr.Button("Refresh")

            self.status_textbox = gr.Textbox(
                label="Status",
                value=self.last_status,
                interactive=False,
            )

            self.play_button.click(
                self.on_play_click,
                inputs=self.song_dropdown,
                outputs=self.status_textbox,
            )
            self.stop_button.click(
                self.on_stop_click,
                outputs=self.status_textbox,
                queue=False,
            )
            self.refresh_button.click(
                self.on_refresh_click,
                outputs=[
                    self.song_dropdown,
                    self.status_textbox,
                ],
            )

    def on_play_click(self, title):
        song = self.manifest.find_by_title(title)
        if song is None:
            self.last_status = "No song selected."
            return self.last_status

        #20260628_kpopmodder: Keep song playback separate from TTS while clearing active speech first.
        try:
            event_manager.trigger(EventType.INTERRUPT)
        except Exception as e:
            log_print(f"[SongPlayer] interrupt before song failed: {e}")

        ok, message = self.playback_controller.play(song)
        self.last_status = message
        return self.last_status

    def on_stop_click(self):
        log_print("[SongPlayer] stop button clicked")#20260629_kpopmodder: Stop must be visible in logs because playback is asynchronous.
        self.playback_controller.stop(join=False)#20260628_kpopmodder: Explicit song stop button path.
        self.last_status = "Stopped."
        return self.last_status

    def on_refresh_click(self):
        self.manifest.load()
        choices = self.manifest.get_titles()
        value = self.manifest.get_initial_title()
        self.last_status = self.manifest.status_text()

        return (
            gr.update(
                choices=choices,
                value=value,
            ),
            self.last_status,
        )

    def is_playing(self):
        return self.playback_controller.is_playing()

    def set_status(self, status):
        self.last_status = str(status or "")

    def send_output(self, output):
        try:
            output = float(output)
        except Exception:
            output = 0.0

        with self.output_lock:
            #20260629_kpopmodder: Snapshot listeners under lock, then run callbacks outside it.
            subscribers = tuple(self.output_event_listeners)

        for subscriber in subscribers:
            try:
                subscriber(output)
            except Exception as e:
                log_print(f"[SongPlayer output listener error] {e}")

    def add_output_event_listener(self, function):
        with self.output_lock:
            if function in self.output_event_listeners:
                return
            self.output_event_listeners.append(function)

    def send_expression(self, expression):#20260628_kpopmodder
        with self.expression_lock:
            #20260629_kpopmodder: Match output dispatch; callbacks run after releasing the lock.
            subscribers = tuple(self.expression_event_listeners)

        for subscriber in subscribers:
            try:
                subscriber(expression)
            except Exception as e:
                log_print(f"[SongPlayer expression listener error] {e}")

    def add_expression_event_listener(self, function):#20260628_kpopmodder
        with self.expression_lock:
            if function in self.expression_event_listeners:
                return
            self.expression_event_listeners.append(function)

    def remove_expression_event_listener(self, function):#20260628_kpopmodder
        with self.expression_lock:
            removed = False
            while function in self.expression_event_listeners:
                self.expression_event_listeners.remove(function)
                removed = True
            return removed

    def remove_output_event_listener(self, function):
        with self.output_lock:
            removed = False
            while function in self.output_event_listeners:
                self.output_event_listeners.remove(function)
                removed = True
            return removed

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True

        try:
            self.playback_controller.stop(join=True)
        except Exception as e:
            log_print(f"[SongPlayer] shutdown stop error: {e}")

        #20260629_kpopmodder: Clear listeners under the same locks used by add/remove/send.
        with self.output_lock:
            self.output_event_listeners.clear()
        with self.expression_lock:
            self.expression_event_listeners.clear()
