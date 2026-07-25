#20260725_kpopmodder: Added base payload builder so text parsing fallback is isolated.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_command_parser import MinecraftCommandParser


class MinecraftBasePayloadBuilder:
    def __init__(self, command_parser: MinecraftCommandParser | None = None):
        self.command_parser = command_parser or MinecraftCommandParser()

    def build(self, command: Any) -> Dict[str, Any]:
        parsed_command = self.command_parser.parse(command)
        if isinstance(parsed_command, dict):
            return parsed_command
        if isinstance(command, dict):
            return dict(command)
        return {"action": command}
