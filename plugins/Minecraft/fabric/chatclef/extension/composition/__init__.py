#20260905_kpopmodder: Export extension component-graph assembly.
from .minecraft_fabric_chatclef_adapter_resolver import (
    MinecraftFabricChatClefAdapterResolver,
)
from .minecraft_fabric_chatclef_extension_component_graph import (
    MinecraftFabricChatClefExtensionComponentGraph,
)
from .minecraft_fabric_chatclef_extension_compatibility_installer import (
    MinecraftFabricChatClefExtensionCompatibilityInstaller,
)

__all__ = (
    "MinecraftFabricChatClefAdapterResolver",
    "MinecraftFabricChatClefExtensionCompatibilityInstaller",
    "MinecraftFabricChatClefExtensionComponentGraph",
)
