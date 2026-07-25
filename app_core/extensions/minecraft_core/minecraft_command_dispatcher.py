#20260725_kpopmodder: Added Minecraft command dispatcher to keep plugin call policy out of lifecycle code.
from __future__ import annotations

from typing import Any

from app_core.extensions.game_extension_contracts import GameCommandDTO

from .minecraft_command_dispatch_result import MinecraftCommandDispatchResult
from .minecraft_command_payload_resolver import MinecraftCommandPayloadResolver
from .minecraft_command_registry import MinecraftCommandRegistry


class MinecraftCommandDispatcher:
    def __init__(
        self,
        registry: MinecraftCommandRegistry | None = None,
        payload_resolver: MinecraftCommandPayloadResolver | None = None,
    ):
        self.registry = registry or MinecraftCommandRegistry()
        self.payload_resolver = payload_resolver or MinecraftCommandPayloadResolver(
            self.registry
        )

    def dispatch(
        self,
        plugin: Any,
        command: Any,
        command_dto: GameCommandDTO,
    ) -> MinecraftCommandDispatchResult:
        payload, action = self.payload_resolver.resolve(command_dto)
        is_text_command = isinstance(command, str)
        if not self.registry.supports(action) and not is_text_command:
            return MinecraftCommandDispatchResult(
                {"ok": False, "action": action or "", "error": "unknown_action"},
                action,
            )

        handler = getattr(plugin, "handle_command", None)
        if not callable(handler):
            return MinecraftCommandDispatchResult(
                {"ok": False, "action": action, "error": "missing_plugin_handler"},
                action,
            )

        if is_text_command:
            return MinecraftCommandDispatchResult(handler(command), action)

        payload["action"] = action
        return MinecraftCommandDispatchResult(handler(payload), action)
