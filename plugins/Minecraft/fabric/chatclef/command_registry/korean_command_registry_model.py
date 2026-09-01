#20260820_kpopmodder: Model ChatClef command readiness axes separately from Korean parsing.
#20260901_kpopmodder: Preserve the legacy model import path after splitting its contracts.
from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandReadinessAxes,
    ChatClefCommandSpec,
)

__all__ = [
    "ChatClefCommandReadinessAxes",
    "ChatClefCommandSpec",
]
