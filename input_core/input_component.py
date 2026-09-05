#20260717_kpopmodder: Moved Input implementation out of the root legacy facade.
#20260905_kpopmodder: Keeps Input as the compatibility facade over focused collaborators.
from plugin_system.interfaces import InputPluginInterface
from plugin_system.selection import PluginSelectionBase

from input_core.component import InputCompatibilityGraphInstaller


class Input(PluginSelectionBase):
    #20260717_kpopmodder: Root Input.py now re-exports this compatibility implementation.
    def __init__(self) -> None:
        super().__init__(InputPluginInterface)
        self._get_compatibility_graph_installer().install()

    def _get_compatibility_graph_installer(self):
        installer = getattr(self, "_input_compatibility_graph_installer", None)
        if installer is None:
            installer = InputCompatibilityGraphInstaller(
                self,
                create_provider_selection_ui_callback=(
                    super().create_plugin_selection_ui
                ),
                create_all_provider_ui_callback=super().create_all_provider_ui,
                select_provider_callback=super().on_dropdown_change,
                base_shutdown_callback=super().shutdown,
            )
            self._input_compatibility_graph_installer = installer
        return installer

    @property
    def output_event_listeners(self):
        return self._get_output_dispatcher().listeners

    @output_event_listeners.setter
    def output_event_listeners(self, value):
        self._get_output_dispatcher().listeners = value

    def _get_output_dispatcher(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_output_dispatcher()
        )

    def create_ui(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_ui_builder()
            .build()
        )

    def create_all_provider_ui(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_provider_selection_coordinator()
            .create_all_provider_ui()
        )

    def on_dropdown_change(self, provider_name):
        return (
            self._get_compatibility_graph_installer()
            .ensure_provider_selection_coordinator()
            .select_provider(provider_name)
        )

    def _sync_provider_listeners(self):
        self._provider_input_event_binding_lifecycle.sync()
        self._provider_binding_request_registry.reconcile()

    def bind_provider_input_event_listener(
        self,
        provider_id,
        listener_factory,
    ):
        """Replace one descriptor-identified provider callback with a composed edge."""
        return self._provider_binding_request_registry.bind(
            provider_id,
            listener_factory,
        )

    def send_output(self, output, *, excluded_listeners=()):
        return self._get_output_dispatcher().send(
            output,
            excluded_listeners=excluded_listeners,
        )

    def add_output_event_listener(self, function):
        return self._get_output_dispatcher().add(function)

    def remove_output_event_listener(self, function):
        return self._get_output_dispatcher().remove(function)

    def shutdown(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_lifecycle_coordinator()
            .shutdown()
        )
