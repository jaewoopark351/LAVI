#20260725_kpopmodder: Added Minecraft command payload/action resolver for extension dispatch.
from __future__ import annotations

from typing import Any, Dict, Tuple

from app_core.extensions.game_extension_contracts import GameCommandDTO

from .minecraft_command_registry import MinecraftCommandRegistry


class MinecraftCommandPayloadResolver:
    def __init__(self, registry: MinecraftCommandRegistry | None = None):
        self.registry = registry or MinecraftCommandRegistry()

    def resolve(self, command_dto: GameCommandDTO) -> Tuple[Dict[str, Any], str]:
        payload = command_dto.to_legacy_dict()
        action = self.registry.normalize_action(
            command_dto.action or self._payload_action(payload)
        )
        return payload, action

    def _payload_action(self, payload: Dict[str, Any]) -> Any:
        nested = payload.get("payload")
        if isinstance(nested, dict):
            return (
                nested.get("action")
                or nested.get("type")
                or nested.get("event")
                or nested.get("event_type")
            )
        return None
