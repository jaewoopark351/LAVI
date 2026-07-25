#20260725_kpopmodder: Added Minecraft command dispatcher to keep plugin call policy out of lifecycle code.
from __future__ import annotations

from typing import Any

from app_core.extensions.game_extension_contracts import GameCommandDTO

from .minecraft_command_dispatch_result import MinecraftCommandDispatchResult
from .minecraft_command_payload_resolver import MinecraftCommandPayloadResolver
from .minecraft_command_registry import MinecraftCommandRegistry
from .minecraft_command_support_guard import MinecraftCommandSupportGuard
from .minecraft_plugin_command_invoker import MinecraftPluginCommandInvoker
from .minecraft_text_command_detector import MinecraftTextCommandDetector
from .minecraft_unknown_action_response_factory import (
    MinecraftUnknownActionResponseFactory,
)


class MinecraftCommandDispatcher:
    def __init__(
        self,
        registry: MinecraftCommandRegistry | None = None,
        payload_resolver: MinecraftCommandPayloadResolver | None = None,
        text_command_detector: MinecraftTextCommandDetector | None = None,
        support_guard: MinecraftCommandSupportGuard | None = None,
        plugin_invoker: MinecraftPluginCommandInvoker | None = None,
        unknown_response_factory: MinecraftUnknownActionResponseFactory | None = None,
    ):
        self.registry = registry or MinecraftCommandRegistry()
        self.payload_resolver = payload_resolver or MinecraftCommandPayloadResolver(
            self.registry
        )
        self.text_command_detector = text_command_detector or MinecraftTextCommandDetector()
        self.support_guard = support_guard or MinecraftCommandSupportGuard(self.registry)
        self.plugin_invoker = plugin_invoker or MinecraftPluginCommandInvoker()
        self.unknown_response_factory = (
            unknown_response_factory or MinecraftUnknownActionResponseFactory()
        )

    def dispatch(
        self,
        plugin: Any,
        command: Any,
        command_dto: GameCommandDTO,
    ) -> MinecraftCommandDispatchResult:
        payload, action = self.payload_resolver.resolve(command_dto)
        is_text_command = self.text_command_detector.is_text_command(command)
        if not self.support_guard.allows(action, is_text_command=is_text_command):
            return self.unknown_response_factory.build(action)

        return self.plugin_invoker.invoke(
            plugin,
            command,
            payload,
            action,
            is_text_command=is_text_command,
        )
