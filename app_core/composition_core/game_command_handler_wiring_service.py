#20260725_kpopmodder: Added focused wiring for LLM game command short-circuit handlers.
from __future__ import annotations

from typing import Any


class GameCommandHandlerWiringService:
    def wire(self, *, llm: Any, game_extension_registry: Any = None) -> None:
        setter = getattr(llm, "set_game_command_handler", None)
        if not callable(setter):
            return
        if game_extension_registry is None:
            setter(None)
            return

        from app_core.extensions.minecraft_core import (
            MinecraftConversationCommandHandler,
        )

        setter(MinecraftConversationCommandHandler(game_extension_registry))
