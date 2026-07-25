#20260725_kpopmodder: Added command payload builder for Minecraft command routing.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_command_parser import MinecraftCommandParser


class MinecraftCommandPayloadBuilder:
    def __init__(self, command_parser: MinecraftCommandParser | None = None):
        self.command_parser = command_parser or MinecraftCommandParser()

    def build(self, command: Any) -> Dict[str, Any]:
        payload = self._base_payload(command)
        nested = payload.get("payload")
        if not isinstance(nested, dict):
            return payload

        merged = dict(nested)
        merged.update({key: value for key, value in payload.items() if key != "payload"})
        return merged

    def action_from(self, payload: Dict[str, Any]) -> str:
        return self._normalize_action(
            payload.get("action")
            or payload.get("type")
            or payload.get("event")
            or payload.get("event_type")
        )

    def _base_payload(self, command: Any) -> Dict[str, Any]:
        parsed_command = self.command_parser.parse(command)
        if isinstance(parsed_command, dict):
            return parsed_command
        if isinstance(command, dict):
            return dict(command)
        return {"action": command}

    def _normalize_action(self, value: Any) -> str:
        return str(value or "").strip().lower().replace("-", "_")
