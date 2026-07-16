from plugin_system.interfaces import TranslationPluginInterface


class NoTranslate(TranslationPluginInterface):
    PLUGIN_METADATA = {
        "id": "NoTranslate",
        "display_name": "No Translate",
        "api_version": "1",
        "dependency_group": "Core",
        "capabilities": ("translation_passthrough",),
        "required_python_packages": (),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": True,
        "supports_cpu": True,
    }

    def translate(self, text):
        return text

    def get_input_language_code(self):
        return 'any'

    def get_output_language_code(self):
        return 'any'
