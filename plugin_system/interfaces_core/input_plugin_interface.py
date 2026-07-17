#20260717_kpopmodder: Added this module to keep one project class per Python file.
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
        for listener in self.input_event_listeners:
            listener(input)
