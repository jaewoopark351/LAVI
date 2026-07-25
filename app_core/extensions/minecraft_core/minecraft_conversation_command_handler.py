#20260725_kpopmodder: Added LLM preflight handler for explicit Minecraft conversation commands.
from __future__ import annotations

from typing import Any

from .minecraft_conversation_command_parser import MinecraftConversationCommandParser
from .minecraft_conversation_command_logger import MinecraftConversationCommandLogger
from .minecraft_conversation_result_formatter import MinecraftConversationResultFormatter
from .minecraft_extension_resolver import MinecraftExtensionResolver
from .minecraft_implicit_conversation_command_parser import (
    MinecraftImplicitConversationCommandParser,
)
from .minecraft_implicit_routing_availability import (
    MinecraftImplicitRoutingAvailability,
)


class MinecraftConversationCommandHandler:
    def __init__(
        self,
        extension_registry: Any,
        parser: MinecraftConversationCommandParser | None = None,
        implicit_parser: MinecraftImplicitConversationCommandParser | None = None,
        formatter: MinecraftConversationResultFormatter | None = None,
        extension_resolver: MinecraftExtensionResolver | None = None,
        implicit_availability: MinecraftImplicitRoutingAvailability | None = None,
        command_logger: MinecraftConversationCommandLogger | None = None,
    ):
        self.extension_registry = extension_registry
        self.parser = parser or MinecraftConversationCommandParser()
        self.implicit_parser = (
            implicit_parser or MinecraftImplicitConversationCommandParser()
        )
        self.formatter = formatter or MinecraftConversationResultFormatter()
        self.extension_resolver = extension_resolver or MinecraftExtensionResolver()
        self.implicit_availability = (
            implicit_availability or MinecraftImplicitRoutingAvailability()
        )
        self.command_logger = command_logger or MinecraftConversationCommandLogger()

    def try_handle(self, text: object) -> str | None:
        route = self.parser.parse(text)
        if route is None:
            extension = self._extension("minecraft")
            if not self.implicit_availability.is_available(extension):
                return None

            route = self.implicit_parser.parse(text)
            if route is None:
                return None

            return self._handle_route(route, extension=extension)

        return self._handle_route(route)

    def _handle_route(self, route, extension: Any | None = None) -> str:
        extension = extension or self._extension(route.game)
        if extension is None:
            return self.formatter.extension_missing(route)

        self.command_logger.routed(route)
        try:
            result = extension.handle_command(route.command)
        except Exception as error:
            return self.formatter.command_failed(route, error)

        return self.formatter.format(route, result)

    def _extension(self, name: str) -> Any:
        return self.extension_resolver.resolve(self.extension_registry, name)
