#20260725_kpopmodder: Added focused logging for Minecraft conversation command routing.
from __future__ import annotations

from core.logger import log_print

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationCommandLogger:
    def routed(self, route: MinecraftConversationCommandRoute) -> None:
        log_print(
            "[MinecraftConversationCommand] routed "
            f"trigger={route.trigger} command={route.command!r}"
        )
