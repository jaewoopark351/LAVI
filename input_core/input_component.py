#20260717_kpopmodder: Moved Input implementation out of the root legacy facade.
from plugin_system.interfaces import InputPluginInterface
from plugin_system.selection import PluginSelectionBase

from core.logger import log_print#20260612_kpopmodder
from input_core.input_event.adapters import ProviderBoundInputEventAdapter
from input_core.input_event.normalization import LaviInputEventNormalizer
from input_core.input_event.provenance import InputProviderSourceResolver


class Input(PluginSelectionBase):
    #20260717_kpopmodder: Root Input.py now re-exports this compatibility implementation.
    def __init__(self) -> None:
        super().__init__(InputPluginInterface)
        self._shutdown = False
        self.output_event_listeners = []
        #20260905_kpopmodder: Provider callbacks retain descriptor-bound identity across every listener sync.
        self._provider_source_resolver = InputProviderSourceResolver()
        self._input_event_normalizer = LaviInputEventNormalizer()
        self._provider_input_event_adapters = {}
        self._sync_provider_listeners()

    def create_ui(self):
        import gradio as gr

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
        #20260905_kpopmodder: Each constructed provider owns one stable descriptor-bound ingress callback.
        for provider in list(self.provider_list):
            plugin = getattr(provider, "plugin", None)
            if plugin is None:
                continue
            listeners = self._provider_listener_list(plugin)
            while self.send_output in listeners:
                listeners.remove(self.send_output)
            adapter = self._provider_input_event_adapters.get(provider)
            if adapter is None:
                adapter = ProviderBoundInputEventAdapter(
                    provider=provider,
                    output_callback=self.send_output,
                    source_resolver=self._provider_source_resolver,
                )
                self._provider_input_event_adapters[provider] = adapter
            while adapter in listeners:
                listeners.remove(adapter)
            if not getattr(provider, "disabled", False):
                listeners.append(adapter)

    def _provider_listener_list(self, plugin):
        listeners = getattr(plugin, "input_event_listeners", None)
        if listeners is None:
            listeners = []
            plugin.input_event_listeners = listeners
        return listeners

    def send_output(self, output):
        event = self._input_event_normalizer.normalize(output)
        log_print(event.text)#20260612_kpopmodder
        for subcriber in list(self.output_event_listeners):
            subcriber(event)

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
            adapter = self._provider_input_event_adapters.get(provider)
            while adapter is not None and adapter in listeners:
                listeners.remove(adapter)

        self._provider_input_event_adapters.clear()
        self.output_event_listeners.clear()
        super().shutdown()
