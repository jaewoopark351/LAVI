#20260725_kpopmodder: Added extractor to isolate equip item parsing.
from __future__ import annotations

from typing import Dict

from .minecraft_command_aliases import DIMENSION_ALIASES, EQUIP_PREFIXES
from .minecraft_text_parse_helpers import first_item_token


class MinecraftEquipRequestExtractor:
    def extract(self, candidate: str) -> Dict[str, object] | None:
        item = first_item_token(
            candidate,
            dimension_aliases=DIMENSION_ALIASES,
            item_prefixes=EQUIP_PREFIXES,
        )
        if not item:
            return None
        return {"item": item}
