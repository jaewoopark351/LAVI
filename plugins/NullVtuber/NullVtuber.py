#20260716_kpopmodder: Null vtuber provider avoids websocket/external app requirements in Core smoke.
from plugin_system.interfaces import VtuberPluginInterface


class NullVtuber(VtuberPluginInterface):
    PLUGIN_METADATA = {
        "id": "NullVtuber",
        "display_name": "Null Vtuber",
        "api_version": "1",
        "dependency_group": "Core",
        "capabilities": ("null_vtuber",),
        "required_python_packages": (),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": True,
        "supports_cpu": True,
    }

    def init(self):
        pass

    def create_ui(self):
        return None

    def set_avatar_data(self, data):
        self.avatar_data = data
