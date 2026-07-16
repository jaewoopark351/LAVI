#20260716_kpopmodder: Null TTS provider avoids model/server/audio output in Core smoke.
from plugin_system.interfaces import TTSPluginInterface


class NullTTS(TTSPluginInterface):
    def init(self):
        pass

    def synthesize(self, text):
        return None

    def create_ui(self):
        return None
