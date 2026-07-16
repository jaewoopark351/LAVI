#20260716_kpopmodder: Null TTS provider avoids model/server/audio output in Core smoke.
from plugin_system.interfaces import TTSPluginInterface


class NullTTS(TTSPluginInterface):
    PLUGIN_METADATA = {
        "id": "NullTTS",
        "display_name": "Null TTS",
        "api_version": "1",
        "dependency_group": "Core",
        "capabilities": ("null_tts",),
        "required_python_packages": (),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": True,
        "supports_cpu": True,
    }

    def init(self):
        pass

    def synthesize(self, text):
        return None

    def create_ui(self):
        return None
