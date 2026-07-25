#20260725_kpopmodder: Added extractor to isolate get-item token and count parsing.
from __future__ import annotations

from typing import Dict

from .minecraft_command_aliases import DIMENSION_ALIASES, ITEM_PREFIXES
from .minecraft_text_parse_helpers import first_item_token, first_positive_number


class MinecraftGetItemRequestExtractor:
    def extract(self, candidate: str) -> Dict[str, object] | None:
        item = first_item_token(
            candidate,
            dimension_aliases=DIMENSION_ALIASES,
            item_prefixes=ITEM_PREFIXES,
        )
        if not item:
            return None
        return {
            "item": item,
            "count": first_positive_number(candidate, default=1),
        }
