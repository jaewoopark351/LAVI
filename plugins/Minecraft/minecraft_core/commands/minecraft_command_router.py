#20260725_kpopmodder: Added this router to keep Minecraft command parsing and dispatch out of the facade.
from __future__ import annotations

from typing import Any, Dict

from ..actions.minecraft_action_service import MinecraftActionService
from .minecraft_command_payload_builder import MinecraftCommandPayloadBuilder
from .minecraft_command_result_formatter import MinecraftCommandResultFormatter


class MinecraftCommandRouter:
    def __init__(
        self,
        action_service: MinecraftActionService,
        payload_builder: MinecraftCommandPayloadBuilder | None = None,
        result_formatter: MinecraftCommandResultFormatter | None = None,
    ):
        self.action_service = action_service
        self.payload_builder = payload_builder or MinecraftCommandPayloadBuilder()
        self.result_formatter = result_formatter or MinecraftCommandResultFormatter()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        payload = self.payload_builder.build(command)
        action = self.payload_builder.action_from(payload)

        if action in {"health", "ping"}:
            return self.result_formatter.with_action(
                self.action_service.health(),
                action,
            )
        if action in {"status", "get_status"}:
            return self.result_formatter.with_action(
                self.action_service.status(),
                action,
            )
        if action in {"inventory", "get_inventory"}:
            return self.result_formatter.with_action(
                self.action_service.inventory(),
                action,
            )
        if action in {"current_action", "actions_current", "get_current_action"}:
            return self.result_formatter.with_action(
                self.action_service.current_action(),
                action,
            )
        if action in {"get_item", "getitem"}:
            return self.result_formatter.with_action(
                self.action_service.get_item(
                    payload.get("item"),
                    payload.get("count", 1),
                ),
                "get_item",
            )
        if action in {"goto", "go_to", "move_to", "travel_to"}:
            return self.result_formatter.with_action(
                self.action_service.goto(
                    payload.get("target"),
                    x=payload.get("x"),
                    y=payload.get("y"),
                    z=payload.get("z"),
                    dimension=payload.get("dimension"),
                ),
                "goto",
            )
        if action in {"stop", "cancel"}:
            return self.result_formatter.with_action(
                self.action_service.stop(),
                action,
            )
        if action == "reload":
            return self.action_service.reload()
        return {
            "ok": False,
            "action": action or "",
            "error": "unknown_action",
        }
