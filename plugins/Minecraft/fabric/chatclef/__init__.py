#20260801_kpopmodder: Isolate the Fabric ChatClef backend package.
#20260801_kpopmodder: Expose the Fabric ChatClef optional plugin entrypoint.
from .minecraft_fabric_chatclef_plugin import MinecraftFabricChatClefPlugin

__all__ = ["MinecraftFabricChatClefPlugin"]
