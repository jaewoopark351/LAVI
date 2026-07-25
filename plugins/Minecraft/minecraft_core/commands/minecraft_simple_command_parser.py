#20260725_kpopmodder: Added parser for direct one-word Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_command_aliases import SIMPLE_ACTION_ALIASES
from .minecraft_text_parse_helpers import build_payload


class MinecraftSimpleCommandParser:
    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        action = SIMPLE_ACTION_ALIASES.get(lowered)
        if not action:
            return None
        return build_payload(action, text)
