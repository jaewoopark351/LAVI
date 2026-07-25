#20260725_kpopmodder: Added orchestration for focused Minecraft text command parsers.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_command_aliases import KNOWN_ACTIONS
from .minecraft_get_item_command_parser import MinecraftGetItemCommandParser
from .minecraft_goto_command_parser import MinecraftGotoCommandParser
from .minecraft_simple_command_parser import MinecraftSimpleCommandParser
from .minecraft_text_parse_helpers import squash_spaces


class MinecraftCommandParser:
    def __init__(
        self,
        simple_parser: MinecraftSimpleCommandParser | None = None,
        goto_parser: MinecraftGotoCommandParser | None = None,
        get_item_parser: MinecraftGetItemCommandParser | None = None,
    ):
        self.simple_parser = simple_parser or MinecraftSimpleCommandParser()
        self.goto_parser = goto_parser or MinecraftGotoCommandParser()
        self.get_item_parser = get_item_parser or MinecraftGetItemCommandParser()

    def parse(self, command: Any) -> Dict[str, Any] | None:
        if isinstance(command, dict):
            return self._parse_mapping(command)
        if not isinstance(command, str):
            return None

        text = command.strip()
        if not text:
            return None

        lowered = squash_spaces(text.lower())
        for parser in (
            self.simple_parser,
            self.goto_parser,
            self.get_item_parser,
        ):
            result = parser.parse(text, lowered)
            if result is not None:
                return result
        return None

    def _parse_mapping(self, command: Dict[str, Any]) -> Dict[str, Any]:
        payload = dict(command)
        action = str(payload.get("action") or "").strip()
        normalized_action = action.lower().replace("-", "_")
        if not action or normalized_action in KNOWN_ACTIONS:
            return payload

        parsed = self.parse(action)
        if not isinstance(parsed, dict):
            return payload

        merged = dict(parsed)
        merged.update({key: value for key, value in payload.items() if key != "action"})
        return merged
