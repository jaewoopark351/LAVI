#20260716_kpopmodder: Null input provider keeps Core smoke offline and side-effect free.
from plugin_system.interfaces import InputPluginInterface


class NullInput(InputPluginInterface):
    def init(self):
        pass

    def create_ui(self):
        return None
