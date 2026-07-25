#20260725_kpopmodder: Added support guard to isolate unknown action validation.
from __future__ import annotations

from .minecraft_command_registry import MinecraftCommandRegistry


class MinecraftCommandSupportGuard:
    def __init__(self, registry: MinecraftCommandRegistry | None = None):
        self.registry = registry or MinecraftCommandRegistry()

    def allows(self, action: str, *, is_text_command: bool) -> bool:
        return is_text_command or self.registry.supports(action)
