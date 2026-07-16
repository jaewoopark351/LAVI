#20260716_kpopmodder: Null input provider keeps Core smoke offline and side-effect free.
from plugin_system.interfaces import InputPluginInterface


class NullInput(InputPluginInterface):
    PLUGIN_METADATA = {
        "id": "NullInput",
        "display_name": "Null Input",
        "api_version": "1",
        "dependency_group": "Core",
        "capabilities": ("null_input",),
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
