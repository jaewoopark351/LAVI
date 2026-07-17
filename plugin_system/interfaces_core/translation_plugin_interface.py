#20260717_kpopmodder: Added this module to keep one project class per Python file.
from plugin_system.runtime_plugin_mixin import RuntimePluginContractMixin


class TranslationPluginInterface(RuntimePluginContractMixin):
    def init(self):
        pass

    def start(self):
        pass

    def stop(self):
        pass

    def translate(self, text):
        raise NotImplementedError

    # Use the two letter language codes from here: https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes
    def get_input_language_code(self):
        raise NotImplementedError

    def get_output_language_code(self):
        raise NotImplementedError

    def create_ui(self):
        pass

    def shutdown(self):
        pass
