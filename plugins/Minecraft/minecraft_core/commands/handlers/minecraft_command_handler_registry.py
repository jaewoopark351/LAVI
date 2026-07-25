#20260725_kpopmodder: Added handler registry so new Minecraft actions register aliases in one place.
from __future__ import annotations

from typing import Dict

from .minecraft_get_item_command_handler import MinecraftGetItemCommandHandler
from .minecraft_goto_command_handler import MinecraftGotoCommandHandler
from .minecraft_read_command_handler import MinecraftReadCommandHandler
from .minecraft_reload_command_handler import MinecraftReloadCommandHandler
from .minecraft_stop_command_handler import MinecraftStopCommandHandler
from .minecraft_unknown_command_handler import MinecraftUnknownCommandHandler


class MinecraftCommandHandlerRegistry:
    def __init__(self, handlers: Dict[str, object], unknown_handler=None):
        self.handlers = handlers
        self.unknown_handler = unknown_handler or MinecraftUnknownCommandHandler()

    @classmethod
    def defaults(cls) -> "MinecraftCommandHandlerRegistry":
        handlers: Dict[str, object] = {}
        cls._register_many(handlers, ("health", "ping"), MinecraftReadCommandHandler("health"))
        cls._register_many(handlers, ("status", "get_status"), MinecraftReadCommandHandler("status"))
        cls._register_many(handlers, ("inventory", "get_inventory"), MinecraftReadCommandHandler("inventory"))
        cls._register_many(
            handlers,
            ("current_action", "actions_current", "get_current_action"),
            MinecraftReadCommandHandler("current_action"),
        )
        cls._register_many(handlers, ("get_item", "getitem"), MinecraftGetItemCommandHandler())
        cls._register_many(
            handlers,
            ("goto", "go_to", "move_to", "travel_to"),
            MinecraftGotoCommandHandler(),
        )
        cls._register_many(handlers, ("stop", "cancel"), MinecraftStopCommandHandler())
        cls._register_many(handlers, ("reload",), MinecraftReloadCommandHandler())
        return cls(handlers)

    def handler_for(self, action: str):
        return self.handlers.get(action, self.unknown_handler)

    @staticmethod
    def _register_many(handlers: Dict[str, object], aliases, handler: object) -> None:
        for alias in aliases:
            handlers[alias] = handler
