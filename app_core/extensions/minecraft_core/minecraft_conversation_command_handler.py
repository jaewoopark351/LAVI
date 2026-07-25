#20260725_kpopmodder: Added LLM preflight handler for explicit Minecraft conversation commands.
from __future__ import annotations

from typing import Any

from .minecraft_conversation_command_parser import MinecraftConversationCommandParser
from .minecraft_conversation_result_formatter import MinecraftConversationResultFormatter


class MinecraftConversationCommandHandler:
    def __init__(
        self,
        extension_registry: Any,
        parser: MinecraftConversationCommandParser | None = None,
        formatter: MinecraftConversationResultFormatter | None = None,
    ):
        self.extension_registry = extension_registry
        self.parser = parser or MinecraftConversationCommandParser()
        self.formatter = formatter or MinecraftConversationResultFormatter()

    def try_handle(self, text: object) -> str | None:
        route = self.parser.parse(text)
        if route is None:
            return None

        extension = self._extension(route.game)
        if extension is None:
            return self.formatter.extension_missing(route)

        try:
            result = extension.handle_command(route.command)
        except Exception as error:
            return self.formatter.command_failed(route, error)

        return self.formatter.format(route, result)

    def _extension(self, name: str) -> Any:
        getter = getattr(self.extension_registry, "get", None)
        if not callable(getter):
            return None
        return getter(name)
