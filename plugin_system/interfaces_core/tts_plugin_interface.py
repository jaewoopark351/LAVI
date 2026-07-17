#20260717_kpopmodder: Added this module to keep one project class per Python file.
from plugin_system.runtime_plugin_mixin import RuntimePluginContractMixin


class TTSPluginInterface(RuntimePluginContractMixin):
    def init(self):
        pass

    def start(self):
        pass

    def stop(self):
        pass

    def synthesize(self, text):
        raise NotImplementedError

    def create_ui(self):
        pass

    def shutdown(self):
        pass
