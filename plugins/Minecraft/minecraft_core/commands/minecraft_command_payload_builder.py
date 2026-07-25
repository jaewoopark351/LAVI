#20260725_kpopmodder: Added command payload builder for Minecraft command routing.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_action_normalizer import MinecraftActionNormalizer
from .minecraft_base_payload_builder import MinecraftBasePayloadBuilder
from .minecraft_command_parser import MinecraftCommandParser
from .minecraft_nested_payload_flattener import MinecraftNestedPayloadFlattener


class MinecraftCommandPayloadBuilder:
    def __init__(
        self,
        command_parser: MinecraftCommandParser | None = None,
        base_payload_builder: MinecraftBasePayloadBuilder | None = None,
        payload_flattener: MinecraftNestedPayloadFlattener | None = None,
        action_normalizer: MinecraftActionNormalizer | None = None,
    ):
        self.base_payload_builder = base_payload_builder or MinecraftBasePayloadBuilder(
            command_parser or MinecraftCommandParser()
        )
        self.payload_flattener = payload_flattener or MinecraftNestedPayloadFlattener()
        self.action_normalizer = action_normalizer or MinecraftActionNormalizer()

    def build(self, command: Any) -> Dict[str, Any]:
        return self.payload_flattener.flatten(self.base_payload_builder.build(command))

    def action_from(self, payload: Dict[str, Any]) -> str:
        return self.action_normalizer.action_from(payload)
