#20260725_kpopmodder: Added Minecraft command payload/action resolver for extension dispatch.
from __future__ import annotations

from typing import Any, Dict, Tuple

from app_core.extensions.game_extension_contracts import GameCommandDTO

from .minecraft_command_registry import MinecraftCommandRegistry
from .minecraft_nested_payload_action_reader import MinecraftNestedPayloadActionReader


class MinecraftCommandPayloadResolver:
    def __init__(
        self,
        registry: MinecraftCommandRegistry | None = None,
        nested_action_reader: MinecraftNestedPayloadActionReader | None = None,
    ):
        self.registry = registry or MinecraftCommandRegistry()
        self.nested_action_reader = nested_action_reader or MinecraftNestedPayloadActionReader()

    def resolve(self, command_dto: GameCommandDTO) -> Tuple[Dict[str, Any], str]:
        payload = command_dto.to_legacy_dict()
        action = self.registry.normalize_action(
            command_dto.action or self.nested_action_reader.read(payload)
        )
        return payload, action
