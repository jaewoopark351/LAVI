#20260716_kpopmodder: Null vtuber provider avoids websocket/external app requirements in Core smoke.
from plugin_system.interfaces import VtuberPluginInterface


class NullVtuber(VtuberPluginInterface):
    def init(self):
        pass

    def create_ui(self):
        return None

    def set_avatar_data(self, data):
        self.avatar_data = data
