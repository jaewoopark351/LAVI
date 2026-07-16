#20260716_kpopmodder: Null LLM provider avoids API/model requirements in Core smoke.
from plugin_system.interfaces import LLMPluginInterface


class NullLLM(LLMPluginInterface):
    PLUGIN_METADATA = {
        "id": "NullLLM",
        "display_name": "Null LLM",
        "api_version": "1",
        "dependency_group": "Core",
        "capabilities": ("null_llm",),
        "required_python_packages": (),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": True,
        "supports_cpu": True,
    }

    def init(self):
        pass

    def predict(self, message, history, system_prompt):
        return ""

    def create_ui(self):
        return None
