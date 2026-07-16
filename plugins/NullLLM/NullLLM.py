#20260716_kpopmodder: Null LLM provider avoids API/model requirements in Core smoke.
from plugin_system.interfaces import LLMPluginInterface


class NullLLM(LLMPluginInterface):
    def init(self):
        pass

    def predict(self, message, history, system_prompt):
        return ""

    def create_ui(self):
        return None
