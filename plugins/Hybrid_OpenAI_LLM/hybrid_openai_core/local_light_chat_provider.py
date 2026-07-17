#20260717_kpopmodder: Isolates disabled local-light provider compatibility behavior.


class LocalLightChatProvider:
    enabled = False

    def __init__(self, settings=None, log_print=None, base_dir=None):
        self.log_print = log_print

    def request_interrupt(self):
        return None

    def unload(self):
        return None

    def stream(self, message, history, system_prompt):
        #20260626_kpopmodder: Hybrid_OpenAI_LLM is OpenAI-only while local_light is disabled.
        raise RuntimeError("local_light provider is disabled")
