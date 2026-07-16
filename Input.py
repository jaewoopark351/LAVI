from plugin_system.interfaces import InputPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase

from core.logger import log_print#20260612_kpopmodder

class Input(PluginSelectionBase):
    def __init__(self) -> None:
        super().__init__(InputPluginInterface)
        self._shutdown = False
        self.output_event_listeners = []
        self._sync_provider_listeners()

    def create_ui(self):
        with gr.Tab("Input"):
            with gr.Blocks():
                super().create_plugin_selection_ui()

            #20260716_kpopmodder: Keep Twitch/Youtube chat panels visible; input providers can run as simultaneous sources.
            super().create_all_provider_ui()
            self._sync_provider_listeners()

    def create_all_provider_ui(self):
        super().create_all_provider_ui()
        self._sync_provider_listeners()

    def on_dropdown_change(self, provider_name):
        selected_name = super().on_dropdown_change(provider_name)
        self._sync_provider_listeners()
        return selected_name

    def _sync_provider_listeners(self):
        #20260716_kpopmodder: Input providers are simultaneous sources, so every loaded provider routes to Input exactly once.
        for provider in list(self.provider_list):
            plugin = getattr(provider, "plugin", None)
            if plugin is None:
                continue
            listeners = self._provider_listener_list(plugin)
            while self.send_output in listeners:
                listeners.remove(self.send_output)
            if not getattr(provider, "disabled", False):
                listeners.append(self.send_output)

    def _provider_listener_list(self, plugin):
        listeners = getattr(plugin, "input_event_listeners", None)
        if listeners is None:
            listeners = []
            plugin.input_event_listeners = listeners
        return listeners

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
