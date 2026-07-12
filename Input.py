import inspect
from plugin_system.interfaces import InputPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase
import os

from core.logger import log_print, debug_print#20260612_kpopmodder

class Input(PluginSelectionBase):
    def __init__(self) -> None:
        super().__init__(InputPluginInterface)
        self._shutdown = False
        if self.send_output not in self.current_plugin.input_event_listeners:
            self.current_plugin.input_event_listeners.append(self.send_output)
        self.output_event_listeners = []

    def create_ui(self):
        with gr.Tab("Input"):
            with gr.Blocks():
                super().create_plugin_selection_ui()

            super().create_plugin_ui()

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
        #20260623_kpopmodder: Remove provider listener links so rebuilt Input instances do not receive stale events.
        for provider in list(self.provider_list):
            plugin = getattr(provider, "plugin", None)
            listeners = getattr(plugin, "input_event_listeners", None)
            if listeners is None:
                continue
            while self.send_output in listeners:
                listeners.remove(self.send_output)

        self.output_event_listeners.clear()
        super().shutdown()
