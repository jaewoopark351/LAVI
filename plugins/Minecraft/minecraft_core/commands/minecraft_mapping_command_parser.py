#20260725_kpopmodder: Added mapping parser so dict merge policy is separate from text parsing.
from __future__ import annotations

from typing import Any, Callable, Dict

from .minecraft_command_aliases import KNOWN_ACTIONS


class MinecraftMappingCommandParser:
    def parse(
        self,
        command: Dict[str, Any],
        text_parser: Callable[[str], Dict[str, Any] | None],
    ) -> Dict[str, Any]:
        payload = dict(command)
        action = str(payload.get("action") or "").strip()
        normalized_action = action.lower().replace("-", "_")
        if not action or normalized_action in KNOWN_ACTIONS:
            return payload

        parsed = text_parser(action)
        if not isinstance(parsed, dict):
            return payload

        merged = dict(parsed)
        merged.update({key: value for key, value in payload.items() if key != "action"})
        return merged
