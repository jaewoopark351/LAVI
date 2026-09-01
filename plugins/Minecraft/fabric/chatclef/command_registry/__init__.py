#20260820_kpopmodder: Expose Python-owned Korean ChatClef command registry contracts.
#20260901_kpopmodder: Export command contracts from their focused canonical package.
from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandReadinessAxes,
    ChatClefCommandSpec,
)
from plugins.Minecraft.fabric.chatclef.command_registry.korean_command_registry import (
    KoreanChatClefCommandRegistry,
)

__all__ = [
    "ChatClefCommandReadinessAxes",
    "ChatClefCommandSpec",
    "KoreanChatClefCommandRegistry",
]
