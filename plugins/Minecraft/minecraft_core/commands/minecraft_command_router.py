#20260725_kpopmodder: Added this router to keep Minecraft command parsing and dispatch out of the facade.
from __future__ import annotations

from typing import Any, Dict

from ..actions.minecraft_action_service import MinecraftActionService
from .handlers.minecraft_command_handler_registry import MinecraftCommandHandlerRegistry
from .minecraft_command_payload_builder import MinecraftCommandPayloadBuilder
from .minecraft_command_preview_builder import MinecraftCommandPreviewBuilder
from .minecraft_command_result_formatter import MinecraftCommandResultFormatter


class MinecraftCommandRouter:
    def __init__(
        self,
        action_service: MinecraftActionService,
        payload_builder: MinecraftCommandPayloadBuilder | None = None,
        preview_builder: MinecraftCommandPreviewBuilder | None = None,
        result_formatter: MinecraftCommandResultFormatter | None = None,
        handler_registry: MinecraftCommandHandlerRegistry | None = None,
    ):
        self.action_service = action_service
        self.payload_builder = payload_builder or MinecraftCommandPayloadBuilder()
        self.preview_builder = preview_builder or MinecraftCommandPreviewBuilder()
        self.result_formatter = result_formatter or MinecraftCommandResultFormatter()
        self.handler_registry = handler_registry or MinecraftCommandHandlerRegistry.defaults()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        payload = self.payload_builder.build(command)
        action = self.payload_builder.action_from(payload)
        handler = self.handler_registry.handler_for(action)
        return handler.handle(
            action,
            payload,
            self.action_service,
            self.result_formatter,
        )

    def preview_command(self, command: Any) -> Dict[str, Any]:
        payload = self.payload_builder.build(command)
        action = self.payload_builder.action_from(payload)
        return self.preview_builder.build(
            action=action,
            payload=payload,
            supported=self.handler_registry.supports(action),
        )
