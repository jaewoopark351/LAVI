#20260717_kpopmodder: Added this module to keep one project class per Python file.
from plugin_system.interfaces_core.avatar_data import AvatarData
from plugin_system.runtime_plugin_mixin import RuntimePluginContractMixin


class VtuberPluginInterface(RuntimePluginContractMixin):
    AvatarData = AvatarData

    def __init__(self):
        #20260718_kpopmodder: Keep avatar state isolated per provider instance while preserving AvatarData alias.
        self.avatar_data = self.AvatarData()

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
