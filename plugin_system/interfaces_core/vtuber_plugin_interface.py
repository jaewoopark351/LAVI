#20260717_kpopmodder: Added this module to keep one project class per Python file.
from plugin_system.runtime_plugin_mixin import RuntimePluginContractMixin


class VtuberPluginInterface(RuntimePluginContractMixin):
    class AvatarData():
        mouth_open = 0
        # TODO current emotion, current pheonome etc

    avatar_data = AvatarData()

    def init(self):
        pass

    def start(self):
        pass

    def stop(self):
        pass

    def create_ui(self):
        pass

    def shutdown(self):
        pass

    def set_avatar_data(self, data):
        self.avatar_data = data
