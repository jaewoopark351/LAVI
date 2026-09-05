#20260905_kpopmodder: Isolate extension adapter resolution from object-graph assembly.
from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)


class MinecraftFabricChatClefAdapterResolver:
    def resolve(self, plugin):
        adapter_factory = getattr(plugin, "create_adapter", None)
        if callable(adapter_factory):
            return adapter_factory()
        return FabricChatClefAdapter()


__all__ = ("MinecraftFabricChatClefAdapterResolver",)
