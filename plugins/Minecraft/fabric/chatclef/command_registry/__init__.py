#20260820_kpopmodder: Expose Python-owned Korean ChatClef command registry contracts.
from plugins.Minecraft.fabric.chatclef.command_registry.korean_command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.korean_command_registry_model import (
    ChatClefCommandReadinessAxes,
    ChatClefCommandSpec,
)

__all__ = [
    "ChatClefCommandReadinessAxes",
    "ChatClefCommandSpec",
    "KoreanChatClefCommandRegistry",
]
