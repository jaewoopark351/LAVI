#20260725_kpopmodder: Added selector to isolate get-item command trigger matching.
from __future__ import annotations

from .minecraft_command_aliases import ITEM_PREFIXES, ITEM_WORDS
from .minecraft_text_parse_helpers import ITEM_TOKEN_PATTERN, NUMBER_PATTERN


class MinecraftGetItemCandidateSelector:
    def select(self, text: str, lowered: str) -> str:
        for prefix in ITEM_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return text[len(prefix) :].strip()
        if any(word in lowered for word in ITEM_WORDS):
            return text
        if self._looks_like_direct_item_request(lowered):
            return text
        return ""

    def _looks_like_direct_item_request(self, lowered: str) -> bool:
        if not NUMBER_PATTERN.search(lowered):
            return False
        return any(
            "_" in token or ":" in token
            for token in ITEM_TOKEN_PATTERN.findall(lowered)
        )
