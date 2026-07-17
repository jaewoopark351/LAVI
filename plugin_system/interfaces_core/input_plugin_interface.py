#20260717_kpopmodder: Added this module to keep one project class per Python file.
import traceback

from core.logger import log_print
from plugin_system.runtime_plugin_mixin import RuntimePluginContractMixin


class InputPluginInterface(RuntimePluginContractMixin):
    def __init__(self):
        self.input_event_listeners = []

    def init(self):
        pass

    def start(self):
        pass

    def stop(self):
        pass

    def create_ui(self):
        pass

    def shutdown(self):
        pass

    # call this function to send your gathered input to next component
    def process_input(self, input: str):
        if not hasattr(self, "input_event_listeners"):
            self.input_event_listeners = []
        for listener in list(self.input_event_listeners):
            try:
                listener(input)
            except (KeyboardInterrupt, SystemExit):
                raise
            except Exception as e:
                listener_name = getattr(
                    listener,
                    "__qualname__",
                    getattr(listener, "__name__", repr(listener)),
                )
                listener_module = getattr(listener, "__module__", "")
                log_print(
                    "[InputPluginInterface] input listener failed: "
                    f"{listener_module}.{listener_name}: {e}\n"
                    f"{traceback.format_exc()}"
                )#20260718_kpopmodder
