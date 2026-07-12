import os
from queue import Queue
import shutil
import threading
import zipfile

import requests
from tqdm import tqdm
from plugin_system.interfaces import VtuberPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase
import LAV_utils
from pydub import AudioSegment
#import simpleaudio as sa
from core.logger import log_print, debug_print#20260612_kpopmodder


class Vtuber(PluginSelectionBase):
    def __init__(self) -> None:
        super().__init__(VtuberPluginInterface)
        self.data = VtuberPluginInterface.AvatarData()
        self.output_event_listeners = []
        self._shutdown = False

    def create_ui(self):
        with gr.Tab("Vtuber"):
            super().create_plugin_selection_ui()
            super().create_plugin_ui()

    def receive_input(self, normalized_volume):
        self.data.mouth_open = normalized_volume
        current_plugin = self.get_current_plugin()
        if current_plugin is not None:
            current_plugin.set_avatar_data(self.data)
        pass

    def receive_song_expression(self, expression):#20260628_kpopmodder
        current_plugin = self.get_current_plugin()
        if current_plugin is None:
            return

        set_song_expression = getattr(current_plugin, "set_song_expression", None)
        if callable(set_song_expression):
            set_song_expression(expression)

    def send_output(self, output):
        log_print(output)#20260612_kpopmodder
        for subcriber in list(self.output_event_listeners):
            subcriber(output)

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
        #20260623_kpopmodder: Leave the avatar mouth closed when app shutdown starts.
        try:
            self.receive_input(0)
        except Exception as e:
            log_print(f"[Vtuber shutdown] mouth close error: {e}")

        self.output_event_listeners.clear()
        super().shutdown()
