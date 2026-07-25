#20260725_kpopmodder: Added parser for get-item style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_command_aliases import DIMENSION_ALIASES, ITEM_PREFIXES, ITEM_WORDS
from .minecraft_text_parse_helpers import (
    build_payload,
    first_item_token,
    first_positive_number,
)


class MinecraftGetItemCommandParser:
    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        candidate = self._candidate_text(text, lowered)
        if not candidate:
            return None

        item = first_item_token(
            candidate,
            dimension_aliases=DIMENSION_ALIASES,
            item_prefixes=ITEM_PREFIXES,
        )
        if not item:
            return None
        payload = build_payload("get_item", text)
        payload.update(
            {
                "item": item,
                "count": first_positive_number(candidate, default=1),
            }
        )
        return payload

    def _candidate_text(self, text: str, lowered: str) -> str:
        for prefix in ITEM_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return text[len(prefix) :].strip()
        if any(word in lowered for word in ITEM_WORDS):
            return text
        return ""
